package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPostingPeriod
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.error.AdPostingErrorCode
import com.ogonggo.core.adposting.implement.dto.AdPostingAppendDto
import com.ogonggo.core.adposting.implement.dto.AdPostingUpdateDto
import com.ogonggo.core.advertiser.implement.AdvertiserAdCountReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractAppender
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractManager
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdPostingReader::class,
    AdPostingAppender::class,
    AdPostingManager::class,
    AdPostingTargetReader::class,
    AdPostingCountReader::class,
    AdvertiserAdCountReader::class,
    AdvertiserContractAppender::class,
    AdvertiserContractReader::class,
    AdvertiserContractManager::class,
)
internal class AdPostingPersistenceTest @Autowired constructor(
    private val reader: AdPostingReader,
    private val appender: AdPostingAppender,
    private val manager: AdPostingManager,
    private val targetReader: AdPostingTargetReader,
    private val adCountReader: AdvertiserAdCountReader,
    private val contractAppender: AdvertiserContractAppender,
    private val contractReader: AdvertiserContractReader,
    private val contractManager: AdvertiserContractManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `등록하면 그 자리 맨 뒤에 붙고 지우면 남은 광고의 순서를 1부터 다시 매긴다`() {
        // given
        val first = append(targetId = 1)
        val second = append(targetId = 2)
        val third = append(targetId = 3)
        append(placement = AdPostingPlacement.JOB_PICK, targetId = 1)

        // when
        manager.delete(reader.readForDelete(first), NOW)
        manager.delete(reader.readForDelete(first), NOW.plusDays(1))
        flushAndClear()

        // then
        val adPostings = reader.readAllByPlacement(AdPostingPlacement.JOB_LIST)
        assertEquals(listOf(second, third), adPostings.map { it.id })
        assertEquals(listOf(1, 2), adPostings.map { it.displayOrder })
        assertEquals(1, reader.readAllByPlacement(AdPostingPlacement.JOB_PICK).single().displayOrder)
        assertEquals(NOW, reader.readForDelete(first).deletedAt)
        val exception = assertThrows(EntityNotFoundException::class.java) { reader.read(first) }
        assertEquals(AdPostingErrorCode.AD_POSTING_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `같은 자리의 같은 대상은 지우기 전까지만 이미 있는 것으로 본다`() {
        // given
        val id = append(targetId = 7)

        // then
        assertTrue(reader.existsInPlacement(AdPostingPlacement.JOB_LIST, 7))
        assertFalse(reader.existsInPlacement(AdPostingPlacement.JOB_PICK, 7))
        manager.delete(reader.readForDelete(id), NOW)
        flushAndClear()
        assertFalse(reader.existsInPlacement(AdPostingPlacement.JOB_LIST, 7))
    }

    @Test
    fun `순서 바꾸기는 받은 순서대로 칸 번호를 다시 매긴다`() {
        // given
        val first = append(targetId = 1)
        val second = append(targetId = 2)
        val third = append(targetId = 3)

        // when
        manager.reorder(AdPostingPlacement.JOB_LIST, listOf(third, first, second))
        flushAndClear()

        // then
        assertEquals(listOf(third, first, second), reader.readAllByPlacement(AdPostingPlacement.JOB_LIST).map { it.id })
    }

    @Test
    fun `광고주별 조회는 자리 선언 순서와 자리 안 순서대로 주고 자리를 주면 그 자리만 준다`() {
        // given
        val calendar = append(placement = AdPostingPlacement.CALENDAR, targetId = 1)
        val listSecond = append(targetId = 2)
        val listFirst = append(targetId = 1)
        append(placement = AdPostingPlacement.JOB_PICK, targetId = 3, advertiserId = OTHER_ADVERTISER_ID)
        manager.reorder(AdPostingPlacement.JOB_LIST, listOf(listFirst, listSecond))
        flushAndClear()

        // then
        assertEquals(listOf(listFirst, listSecond, calendar), reader.readAllByAdvertiser(ADVERTISER_ID, null).map { it.id })
        assertEquals(listOf(calendar), reader.readAllByAdvertiser(ADVERTISER_ID, AdPostingPlacement.CALENDAR).map { it.id })
    }

    @Test
    fun `대상은 게시 중이고 마감되지 않았으며 모집 종료 일시가 지나지 않은 채용공고만 나갈 수 있다`() {
        // given
        val open = job()
        val hidden = job(publicationStatus = JobPublicationStatus.HIDDEN)
        val closed = job().also { it.close(NOW.minusDays(1)) }
        val expired = job(recruitmentEndAt = NOW.minusSeconds(1))
        val endsNow = job(recruitmentEndAt = NOW)
        val deleted = job().also { it.delete(NOW.minusDays(1)) }
        val ids = listOf(open, hidden, closed, expired, endsNow, deleted).map { job -> append(targetId = job.id!!) }
        flushAndClear()

        // when
        val targets = targetReader.read(reader.readAllByPlacement(AdPostingPlacement.JOB_LIST), NOW)

        // then
        assertEquals(listOf(true, false, false, false, true, false), ids.map { targets.getValue(it).available })
        assertEquals("백엔드 개발자", targets.getValue(ids[0]).title)
        assertEquals("", targets.getValue(ids[5]).title)
        assertEquals("", targets.getValue(ids[5]).companyName)
        assertTrue(targetReader.exists(AdPostingPlacement.JOB_LIST.targetType, hidden.id!!))
        assertFalse(targetReader.exists(AdPostingPlacement.JOB_LIST.targetType, deleted.id!!))
    }

    @Test
    fun `부트캠프는 게시 중이고 게시 기간 안이며 모집 중인 것만 나갈 수 있다`() {
        // given
        val recruiting = bootcamp()
        val draft = bootcamp(status = BootcampRecruitmentStatus.DRAFT)
        val unpublished = bootcamp(publicationStatus = BootcampPublicationStatus.HIDDEN)
        val publicationEnded = bootcamp(publicationEndAt = NOW.minusSeconds(1))
        val ids = listOf(recruiting, draft, unpublished, publicationEnded).map { bootcamp ->
            append(placement = AdPostingPlacement.BOOTCAMP_LIST, targetId = bootcamp.id!!)
        }
        flushAndClear()

        // when
        val targets = targetReader.read(reader.readAllByPlacement(AdPostingPlacement.BOOTCAMP_LIST), NOW)

        // then
        assertEquals(listOf(true, false, false, false), ids.map { targets.getValue(it).available })
        assertEquals("백엔드 부트캠프", targets.getValue(ids[0]).bootcamp?.title)
    }

    @Test
    fun `광고주 광고 수는 노출 중이고 대상이 나갈 수 있는 것만 노출 중으로 세고 가장 늦은 노출 끝을 준다`() {
        // given
        val open = job()
        val hidden = job(publicationStatus = JobPublicationStatus.HIDDEN)
        append(targetId = open.id!!, period = AdPostingPeriod(null, NOW.plusDays(3)))
        append(targetId = hidden.id!!, period = AdPostingPeriod(null, NOW.plusDays(5)))
        append(placement = AdPostingPlacement.JOB_PICK, targetId = open.id!!, enabled = false)
        val deleted = append(placement = AdPostingPlacement.CALENDAR, targetId = open.id!!, period = AdPostingPeriod(null, NOW.plusDays(9)))
        manager.delete(reader.readForDelete(deleted), NOW)
        flushAndClear()

        // when
        val counts = adCountReader.readByAdvertisers(listOf(ADVERTISER_ID, OTHER_ADVERTISER_ID), NOW)

        // then
        val count = counts.getValue(ADVERTISER_ID)
        assertEquals(3, count.adPostingCount)
        assertEquals(1, count.activeAdPostingCount)
        assertEquals(NOW.plusDays(5), count.latestEndAt)
        assertEquals(0, counts.getValue(OTHER_ADVERTISER_ID).adPostingCount)
        assertNull(counts.getValue(OTHER_ADVERTISER_ID).latestEndAt)
    }

    @Test
    fun `계약을 지우면 이어 둔 광고 공고는 남기고 계약 연결만 끊으며 계약별 광고 공고 수를 센다`() {
        // given
        val contractId = checkNotNull(contractAppender.append(AdvertiserContractAppendDto(ADVERTISER_ID, contractTerms())).id)
        val linked = append(targetId = 1, contractId = contractId)
        val linkedDeleted = append(targetId = 2, contractId = contractId)
        manager.delete(reader.readForDelete(linkedDeleted), NOW)
        flushAndClear()
        assertEquals(1, adCountReader.readByContracts(listOf(contractId)).getValue(contractId).adPostingCount)

        // when
        contractManager.delete(contractReader.readForDelete(contractId), NOW)
        flushAndClear()

        // then
        assertNull(reader.read(linked).contractId)
        assertNull(reader.readForDelete(linkedDeleted).contractId)
        assertEquals(0, adCountReader.readByContracts(listOf(contractId)).getValue(contractId).adPostingCount)
    }

    @Test
    fun `수정은 광고주·계약·기간·켜짐을 바꾸고 자리와 대상은 그대로 둔다`() {
        // given
        val id = append(targetId = 1)

        // when
        manager.change(
            reader.readForUpdate(id),
            AdPostingUpdateDto(advertiserId = OTHER_ADVERTISER_ID, contractId = null, period = AdPostingPeriod(NOW, null), enabled = false),
        )
        flushAndClear()

        // then
        val adPosting = reader.read(id)
        assertEquals(OTHER_ADVERTISER_ID, adPosting.advertiserId)
        assertEquals(NOW, adPosting.startAt)
        assertNull(adPosting.endAt)
        assertFalse(adPosting.enabled)
        assertEquals(AdPostingPlacement.JOB_LIST, adPosting.placement)
        assertEquals(1L, adPosting.targetId)
    }

    private fun append(
        placement: AdPostingPlacement = AdPostingPlacement.JOB_LIST,
        targetId: Long,
        advertiserId: Long = ADVERTISER_ID,
        contractId: Long? = null,
        period: AdPostingPeriod = AdPostingPeriod(null, null),
        enabled: Boolean = true,
    ): Long {
        val adPosting = appender.append(
            AdPostingAppendDto(
                placement = placement,
                targetId = targetId,
                advertiserId = advertiserId,
                contractId = contractId,
                period = period,
                enabled = enabled,
            ),
        )
        entityManager.flush()
        return checkNotNull(adPosting.id)
    }

    private fun job(
        publicationStatus: JobPublicationStatus = JobPublicationStatus.PUBLISHED,
        recruitmentEndAt: LocalDateTime? = null,
    ): Job = Job(
        companyName = "고위드",
        title = "백엔드 개발자",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.EXPERIENCED,
        experienceMinYears = 1,
        recruitmentType = if (recruitmentEndAt == null) JobRecruitmentType.ALWAYS_OPEN else JobRecruitmentType.PERIOD,
        recruitmentStartAt = recruitmentEndAt?.minusDays(30),
        recruitmentEndAt = recruitmentEndAt,
        publicationStatus = publicationStatus,
        sourceUrl = "https://example.com/jobs/1",
        listSortKey = 0L,
        now = NOW.minusDays(10),
    ).also(entityManager::persist)

    private fun bootcamp(
        status: BootcampRecruitmentStatus = BootcampRecruitmentStatus.RECRUITING,
        publicationStatus: BootcampPublicationStatus = BootcampPublicationStatus.PUBLISHED,
        publicationEndAt: LocalDateTime? = null,
    ): Bootcamp = Bootcamp(
        companyName = "고위드",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = NOW.minusDays(10),
        recruitmentEndAt = NOW.plusDays(10),
        programStartDate = LocalDate.of(2026, 12, 1),
        programEndDate = LocalDate.of(2027, 3, 1),
        tuitionType = BootcampTuitionType.FREE,
        representativeImageUrl = null,
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        publicationEndAt = publicationEndAt,
        status = status,
        publicationStatus = publicationStatus,
    ).also(entityManager::persist)

    private fun contractTerms() = AdvertiserContractTerms(
        name = "11월 목록 광고",
        startAt = NOW.minusDays(5),
        endAt = NOW.plusDays(25),
        amount = 500_000,
        vatIncluded = true,
        paid = false,
        paidDate = null,
        products = emptyList(),
        memo = null,
    )

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private companion object {
        const val ADVERTISER_ID = 10L
        const val OTHER_ADVERTISER_ID = 11L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
