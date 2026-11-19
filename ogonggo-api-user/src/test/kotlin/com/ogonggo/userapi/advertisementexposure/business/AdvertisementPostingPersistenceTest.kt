package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** 공개 광고 공고는 노출 조건(켜짐·기간)과 대상 조건(게시·모집 중)을 둘 다 DB 조회로 거르므로 실제 저장소로 확인한다. */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-advertisement-posting;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        "spring.mail.host=localhost",
    ],
)
class AdvertisementPostingPersistenceTest @Autowired constructor(
    private val service: AdvertisementExposureService,
    private val productAppender: AdvertisementProductAppender,
    private val exposureAppender: AdvertisementExposureAppender,
    private val jobAppender: JobAppender,
    private val jobReader: JobReader,
    private val jobManager: JobManager,
    private val bootcampAppender: BootcampAppender,
    private val bootcampReader: BootcampReader,
    private val bootcampManager: BootcampManager,
    private val transaction: TransactionTemplate,
) {

    @Test
    fun `켜져 있고 기간 안인 노출 중 대상 공고가 게시·모집 중인 것만 순서대로 준다`() {
        // given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val productId = product(AdvertisementProductPosition.MYPAGE_SCRAP)
        val first = job("첫 광고 공고")
        val second = job("둘째 광고 공고")
        val firstExposure = exposure(productId, AdvertisementExposureTargetType.JOB, first)
        exposure(productId, AdvertisementExposureTargetType.JOB, job("마감 공고").also { jobManager.close(jobReader.read(it), now) })
        exposure(productId, AdvertisementExposureTargetType.JOB, job("숨긴 공고", JobPublicationStatus.HIDDEN))
        exposure(productId, AdvertisementExposureTargetType.JOB, job("꺼진 노출"), exposed = false)
        exposure(
            productId,
            AdvertisementExposureTargetType.JOB,
            job("기간 전 노출"),
            period = AdvertisementExposurePeriod(now.plusDays(1), now.plusDays(2)),
        )
        exposure(
            productId,
            AdvertisementExposureTargetType.JOB,
            job("기간 끝난 노출"),
            period = AdvertisementExposurePeriod(now.minusDays(2), now.minusDays(1)),
        )
        val secondExposure = exposure(
            productId,
            AdvertisementExposureTargetType.JOB,
            second,
            period = AdvertisementExposurePeriod(now.minusDays(1), now.plusDays(1)),
        )

        // when
        val postings = service.getShownPostings(userId = null, position = AdvertisementProductPosition.MYPAGE_SCRAP)

        // then
        assertEquals(listOf(firstExposure, secondExposure), postings.map { it.exposureId })
        assertEquals(listOf(first, second), postings.map { it.job?.id })
        assertEquals(listOf("첫 광고 공고", "둘째 광고 공고"), postings.map { it.job?.title })
        assertEquals(listOf(1, 7), postings.map { it.displayOrder })
        assertEquals(listOf(null, now.minusDays(1)), postings.map { it.startAt })
        assertEquals(listOf(null, now.plusDays(1)), postings.map { it.endAt })
        postings.forEach {
            assertEquals(AdvertisementProductPosition.MYPAGE_SCRAP, it.position)
            assertNull(it.jobField)
            assertNull(it.recommendationTitle)
            assertNull(it.recommendationDescription)
            assertEquals(AdvertisementExposureTargetType.JOB, it.targetType)
            assertEquals(false, it.job?.bookmarked)
            assertNull(it.bootcamp)
        }
    }

    @Test
    fun `채용공고 목록 자리는 요청한 직군 탭의 노출만 직군과 함께 준다`() {
        // given
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val development = exposure(productId, AdvertisementExposureTargetType.JOB, job("개발 공고"), jobField = JobField.IT_DEVELOPMENT)
        exposure(productId, AdvertisementExposureTargetType.JOB, job("게임 공고"), jobField = JobField.GAME)

        // when
        val postings = service.getShownPostings(
            userId = null,
            position = AdvertisementProductPosition.JOB_LIST,
            jobField = JobField.IT_DEVELOPMENT,
        )

        // then
        assertEquals(listOf(development), postings.map { it.exposureId })
        assertEquals(JobField.IT_DEVELOPMENT, postings.single().jobField)
        assertEquals("개발 공고", postings.single().job?.title)
    }

    @Test
    fun `채용공고 목록 자리에 직군 없이 조회하면 전체 탭 노출만 준다`() {
        // given
        val productId = product(AdvertisementProductPosition.JOB_LIST)
        val allTab = exposure(productId, AdvertisementExposureTargetType.JOB, job("전체 탭 공고"))
        exposure(productId, AdvertisementExposureTargetType.JOB, job("게임 탭 공고"), jobField = JobField.GAME)

        // when
        val postings = service.getShownPostings(userId = null, position = AdvertisementProductPosition.JOB_LIST)

        // then
        assertEquals(listOf(allTab), postings.map { it.exposureId })
        assertNull(postings.single().jobField)
        assertEquals("전체 탭 공고", postings.single().job?.title)
    }

    @Test
    fun `오공고 PICK 자리는 그 자리에 등록한 채용공고 요약을 추천 문구와 함께 준다`() {
        // given
        val pick = product(AdvertisementProductPosition.JOB_PICK)
        val other = product(AdvertisementProductPosition.JOB_DETAIL_SIDE)
        val picked = job("오공고 PICK 공고")
        val shown = exposure(
            pick,
            AdvertisementExposureTargetType.JOB,
            picked,
            recommendation = "이번 주 추천" to "신입도 지원할 수 있는 공고예요.",
        )
        exposure(other, AdvertisementExposureTargetType.JOB, job("다른 자리 공고"))

        // when
        val postings = service.getShownPostings(userId = null, position = AdvertisementProductPosition.JOB_PICK)

        // then
        assertEquals(listOf(shown), postings.map { it.exposureId })
        assertEquals(AdvertisementProductPosition.JOB_PICK, postings.single().position)
        assertEquals(picked, postings.single().job?.id)
        assertEquals(true, postings.single().adLabel)
        assertEquals("이번 주 추천", postings.single().recommendationTitle)
        assertEquals("신입도 지원할 수 있는 공고예요.", postings.single().recommendationDescription)
        assertNull(postings.single().bootcamp)
    }

    @Test
    fun `부트캠프 목록 자리는 게시·모집 중인 부트캠프 요약만 주고 마감·숨김 부트캠프는 뺀다`() {
        // given
        val productId = product(AdvertisementProductPosition.BOOTCAMP_LIST)
        val recruiting = bootcamp("모집 중 부트캠프")
        val closed = bootcamp("마감 부트캠프")
        bootcampManager.close(bootcampReader.read(closed), LocalDateTime.now())
        val hidden = bootcamp("숨긴 부트캠프", BootcampPublicationStatus.HIDDEN)
        val shown = exposure(productId, AdvertisementExposureTargetType.BOOTCAMP, recruiting)
        exposure(productId, AdvertisementExposureTargetType.BOOTCAMP, closed)
        exposure(productId, AdvertisementExposureTargetType.BOOTCAMP, hidden)

        // when
        val postings = service.getShownPostings(userId = null, position = AdvertisementProductPosition.BOOTCAMP_LIST)

        // then
        assertEquals(listOf(shown), postings.map { it.exposureId })
        assertEquals(AdvertisementProductPosition.BOOTCAMP_LIST, postings.single().position)
        assertEquals(AdvertisementExposureTargetType.BOOTCAMP, postings.single().targetType)
        assertEquals(recruiting, postings.single().bootcamp?.id)
        assertEquals("모집 중 부트캠프", postings.single().bootcamp?.title)
        assertNull(postings.single().job)
    }

    @Test
    fun `부트캠프 상세 옆과 마이페이지 스크랩 부트캠프 칸은 게시·모집 중인 부트캠프 요약만 준다`() {
        listOf(
            AdvertisementProductPosition.BOOTCAMP_DETAIL_SIDE,
            AdvertisementProductPosition.BOOTCAMP_MYPAGE_SCRAP,
        ).forEach { position ->
            // given
            val productId = product(position)
            val recruiting = bootcamp("모집 중 ${position.desc}")
            val hidden = bootcamp("숨긴 ${position.desc}", BootcampPublicationStatus.HIDDEN)
            val shown = exposure(productId, AdvertisementExposureTargetType.BOOTCAMP, recruiting)
            exposure(productId, AdvertisementExposureTargetType.BOOTCAMP, hidden)

            // when
            val postings = service.getShownPostings(userId = null, position = position)

            // then
            assertEquals(listOf(shown), postings.map { it.exposureId })
            assertEquals(position, postings.single().position)
            assertEquals(AdvertisementExposureTargetType.BOOTCAMP, postings.single().targetType)
            assertEquals(recruiting, postings.single().bootcamp?.id)
            assertNull(postings.single().job)
            assertNull(postings.single().jobField)
            assertNull(postings.single().recommendationTitle)
        }
    }

    private fun product(position: AdvertisementProductPosition): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.JOB_POSTING,
            position = position,
            name = "광고 공고 ${position.desc}",
            price = null,
        ),
    )

    private fun exposure(
        productId: Long,
        targetType: AdvertisementExposureTargetType,
        targetId: Long,
        exposed: Boolean = true,
        period: AdvertisementExposurePeriod = AdvertisementExposurePeriod.ALWAYS,
        jobField: JobField? = null,
        recommendation: Pair<String, String>? = null,
    ): Long = checkNotNull(
        transaction.execute {
            exposureAppender.append(
                AdvertisementExposureAppendDto(
                    productId = productId,
                    contractId = null,
                    content = AdvertisementExposureContent(
                        targetType = targetType,
                        targetId = targetId,
                        jobField = jobField,
                        recommendationTitle = recommendation?.first,
                        recommendationDescription = recommendation?.second,
                    ),
                    adLabel = true,
                    period = period,
                    exposed = exposed,
                ),
            )
        },
    )

    private fun job(title: String, publicationStatus: JobPublicationStatus = JobPublicationStatus.PUBLISHED): Long = checkNotNull(
        jobAppender.append(
            JobAppendDto(
                companyName = "오공고",
                title = title,
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.EXPERIENCED,
                recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
                responsibilities = "서버 개발",
                publicationStatus = publicationStatus,
            ),
        ).id,
    )

    private fun bootcamp(title: String, publicationStatus: BootcampPublicationStatus = BootcampPublicationStatus.PUBLISHED): Long {
        val bootcamp = bootcampAppender.append(
            BootcampAppendDto(
                companyName = "오공고 교육사",
                title = title,
                programType = "개발",
                operationType = BootcampOperationType.ONLINE,
                recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
                programStartDate = LocalDate.of(2026, 9, 1),
                programEndDate = LocalDate.of(2026, 12, 1),
                tuitionType = BootcampTuitionType.FREE,
                representativeImageUrl = null,
                shortDescription = "백엔드 개발자로 성장하는 12주",
                content = "부트캠프 상세 내용",
                applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
                applicationUrl = "https://example.com/apply",
                publicationStatus = publicationStatus,
            ),
        )
        bootcampManager.startRecruitment(bootcamp)
        return checkNotNull(bootcamp.id)
    }
}
