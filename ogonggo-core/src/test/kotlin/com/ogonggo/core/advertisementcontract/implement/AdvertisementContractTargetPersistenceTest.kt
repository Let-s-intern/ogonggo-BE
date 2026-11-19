package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.jpa.CoreJpaConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdvertisementContractAppender::class, AdvertisementContractTargetReader::class, AdvertisementContractTargetManager::class)
internal class AdvertisementContractTargetPersistenceTest @Autowired constructor(
    private val appender: AdvertisementContractAppender,
    private val targetReader: AdvertisementContractTargetReader,
    private val targetManager: AdvertisementContractTargetManager,
    private val contractRepository: AdvertisementContractJpaRepository,
) {

    @MockBean
    private lateinit var jobReader: JobReader

    @MockBean
    private lateinit var bootcampReader: BootcampReader

    @Test
    fun `연결한 대상은 연결 순서로 보이고 해제하면 빠지며 다시 연결하면 되살아난다`() {
        // given
        givenJob(1L, "백엔드 개발자")
        givenJob(2L, "프론트엔드 개발자")
        val contract = contractRepository.findById(appendForm()).get()

        // when
        targetManager.add(contract, AdvertisementContractPromotionTarget.JOB, 1L)
        targetManager.add(contract, AdvertisementContractPromotionTarget.JOB, 2L)
        targetManager.remove(contract, AdvertisementContractPromotionTarget.JOB, 1L, NOW)
        targetManager.remove(contract, AdvertisementContractPromotionTarget.JOB, 1L, NOW)
        val afterRemove = targetReader.readTargets(contract).map { it.targetId }
        targetManager.add(contract, AdvertisementContractPromotionTarget.JOB, 1L)

        // then
        assertEquals(listOf(2L), afterRemove)
        assertEquals(listOf(1L, 2L), targetReader.readTargets(contract).map { it.targetId })
        assertEquals("백엔드 개발자", targetReader.readTargets(contract).first().title)
    }

    @Test
    fun `이미 연결한 대상과 비즈니스 공고 계약의 공고는 중복이라 409다`() {
        // given
        givenJob(1L, "백엔드 개발자")
        val form = contractRepository.findById(appendForm()).get()
        targetManager.add(form, AdvertisementContractPromotionTarget.JOB, 1L)
        val posting = contractRepository.findById(appendPosting(1L)).get()

        // when
        val duplicated = assertThrows<ConflictException> { targetManager.add(form, AdvertisementContractPromotionTarget.JOB, 1L) }
        val fixedDuplicated = assertThrows<ConflictException> { targetManager.add(posting, AdvertisementContractPromotionTarget.JOB, 1L) }
        val fixedRemoved = assertThrows<ConflictException> { targetManager.remove(posting, AdvertisementContractPromotionTarget.JOB, 1L, NOW) }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_DUPLICATED, duplicated.errorCode)
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_DUPLICATED, fixedDuplicated.errorCode)
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_TARGET_FIXED, fixedRemoved.errorCode)
        assertEquals(listOf(true), targetReader.readTargets(posting).map { it.fixed })
    }

    @Test
    fun `없거나 지운 공고는 연결하지 않는다`() {
        // given
        Mockito.`when`(jobReader.read(404L)).thenThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))
        val contract = contractRepository.findById(appendForm()).get()

        // when
        val exception = assertThrows<EntityNotFoundException> { targetManager.add(contract, AdvertisementContractPromotionTarget.JOB, 404L) }

        // then
        assertEquals(JobErrorCode.JOB_NOT_FOUND, exception.errorCode)
        assertEquals(emptyList<Long>(), targetReader.readTargets(contract).map { it.targetId })
    }

    private fun givenJob(id: Long, title: String) {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.title).thenReturn(title)
        Mockito.`when`(job.companyName).thenReturn("뱅크")
        Mockito.`when`(jobReader.read(id)).thenReturn(job)
        Mockito.`when`(jobReader.readIncludingDeleted(id)).thenReturn(job)
    }

    private fun appendForm(): Long = appender.appendFromForm(
        AdvertisementContractFormAppendDto(
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "렛츠커리어",
            recruitmentTitle = "브랜드 마케팅 인턴 모집",
            recruitmentUrl = "https://example.com/recruit",
            recruitmentDeadline = LocalDate.of(2026, 12, 20),
            purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
            budgetAmount = null,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "010-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        ),
    )

    private fun appendPosting(jobId: Long): Long = appender.appendForCompanyPosting(
        AdvertisementContractCompanyPostingAppendDto(
            userId = 7L,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            targetId = jobId,
            companyName = "뱅크",
            recruitmentTitle = "백엔드 개발자",
            recruitmentDeadline = null,
        ),
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 19, 10, 0)
    }
}
