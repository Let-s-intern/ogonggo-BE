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
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
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
import java.time.LocalDateTime

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
    private val transaction: TransactionTemplate,
) {

    @Test
    fun `켜져 있고 기간 안인 노출 중 대상 공고가 게시·모집 중인 것만 순서대로 준다`() {
        // given
        val now = LocalDateTime.now()
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
        postings.forEach {
            assertEquals(AdvertisementProductPosition.MYPAGE_SCRAP, it.position)
            assertEquals(AdvertisementExposureTargetType.JOB, it.targetType)
            assertEquals(false, it.job?.bookmarked)
            assertNull(it.bootcamp)
        }
    }

    @Test
    fun `오공고 PICK 자리는 그 자리에 등록한 채용공고 요약만 준다`() {
        // given
        val pick = product(AdvertisementProductPosition.JOB_PICK)
        val other = product(AdvertisementProductPosition.CALENDAR_DAY)
        val picked = job("오공고 PICK 공고")
        val shown = exposure(pick, AdvertisementExposureTargetType.JOB, picked)
        exposure(other, AdvertisementExposureTargetType.JOB, job("다른 자리 공고"))

        // when
        val postings = service.getShownPostings(userId = null, position = AdvertisementProductPosition.JOB_PICK)

        // then
        assertEquals(listOf(shown), postings.map { it.exposureId })
        assertEquals(AdvertisementProductPosition.JOB_PICK, postings.single().position)
        assertEquals(picked, postings.single().job?.id)
        assertEquals(true, postings.single().adLabel)
        assertNull(postings.single().bootcamp)
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
    ): Long = checkNotNull(
        transaction.execute {
            exposureAppender.append(
                AdvertisementExposureAppendDto(
                    productId = productId,
                    contractId = null,
                    content = AdvertisementExposureContent(targetType = targetType, targetId = targetId),
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
}
