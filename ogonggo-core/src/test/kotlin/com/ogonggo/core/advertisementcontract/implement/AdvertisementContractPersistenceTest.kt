package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractQueryRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.contentreview.persistence.ContentRejectionQueryRepository
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import com.ogonggo.core.contentreview.implement.ContentRejectionReader
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertisementContractAppender::class,
    AdvertisementContractManager::class,
    AdvertisementContractReader::class,
    AdvertisementContractQueryRepository::class,
    AdvertisementProductAppender::class,
    AdvertisementProductManager::class,
    AdvertisementProductReader::class,
    ContentRejectionReader::class,
    ContentRejectionManager::class,
    ContentRejectionQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    JobQueryRepository::class,
)
internal class AdvertisementContractPersistenceTest @Autowired constructor(
    private val appender: AdvertisementContractAppender,
    private val manager: AdvertisementContractManager,
    private val reader: AdvertisementContractReader,
    private val repository: AdvertisementContractJpaRepository,
    private val productAppender: AdvertisementProductAppender,
    private val productManager: AdvertisementProductManager,
    private val productReader: AdvertisementProductReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `비즈니스 공고 계약은 공고 검수 상태를 따른다`() {
        // given
        val job = jobAppender.append(companyJob())
        val contractId = appendContract(job)

        // when & then
        jobManager.rejectReview(job, "급여 조건이 비어 있습니다.", NOW)
        assertEquals(AdvertisementContractStatus.REJECTED, statusOf(contractId))

        jobManager.requestReview(job, NOW.plusHours(1))
        assertEquals(AdvertisementContractStatus.REQUESTED, statusOf(contractId))

        jobManager.approveReview(job, NOW.plusHours(2))
        assertEquals(AdvertisementContractStatus.APPROVED, statusOf(contractId))
    }

    @Test
    fun `계약이 없는 비즈니스 공고도 검수할 수 있다`() {
        // given
        val job = jobAppender.append(companyJob())

        // when
        jobManager.approveReview(job, NOW)

        // then
        assertEquals(ContentReviewStatus.APPROVED, job.reviewStatus)
        assertEquals(0L, repository.count())
    }

    @Test
    fun `공고 하나에 비즈니스 공고 계약은 하나만 둔다`() {
        // given
        val job = jobAppender.append(companyJob())
        appendContract(job)

        // when & then
        assertThrows(DataIntegrityViolationException::class.java) {
            appendContract(job)
            repository.flush()
        }
    }

    @Test
    fun `신청 폼 계약은 고른 카테고리를 저장하고 다시 읽는다`() {
        // given
        val saved = repository.save(formContract(setOf(AdvertisementProductCategory.BANNER, AdvertisementProductCategory.KAKAO_1)))
        repository.flush()
        entityManager.clear()

        // when
        val found = repository.findById(checkNotNull(saved.id)).get()

        // then
        assertEquals(setOf(AdvertisementProductCategory.BANNER, AdvertisementProductCategory.KAKAO_1), found.categories)
        assertEquals(AdvertisementContractStatus.REQUESTED, found.status)
    }

    @Test
    fun `목록은 최근 계약부터 주고 경로, 상태, 카테고리, 기업명으로 거른다`() {
        // given
        val banner = appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "렛츠커리어")
        val kakao = appendForm(setOf(AdvertisementProductCategory.KAKAO_1, AdvertisementProductCategory.BANNER), companyName = "코드스테이션")
        val posting = appendContract(jobAppender.append(companyJob()))
        manager.changeStatus(reader.readForUpdate(kakao), AdvertisementContractStatus.NEGOTIATING)
        repository.flush()
        entityManager.clear()

        fun ids(condition: AdvertisementContractSearchCondition) =
            reader.readPage(condition, page = 0, size = 10).advertisementContracts.map { it.id }

        // when & then
        assertEquals(listOf(posting, kakao, banner), ids(AdvertisementContractSearchCondition()))
        assertEquals(listOf(kakao, banner), ids(AdvertisementContractSearchCondition(source = AdvertisementContractSource.APPLICATION_FORM)))
        assertEquals(listOf(kakao), ids(AdvertisementContractSearchCondition(statuses = setOf(AdvertisementContractStatus.NEGOTIATING))))
        assertEquals(listOf(kakao), ids(AdvertisementContractSearchCondition(category = AdvertisementProductCategory.KAKAO_1)))
        assertEquals(listOf(kakao, banner), ids(AdvertisementContractSearchCondition(category = AdvertisementProductCategory.BANNER)))
        assertEquals(listOf(kakao), ids(AdvertisementContractSearchCondition(companyName = "스테이션")))
    }

    @Test
    fun `계약에서 뺀 상품은 주지 않고, 지운 상품은 이름과 함께 지웠다고 준다`() {
        // given
        val contract = reader.readForUpdate(appendForm(setOf(AdvertisementProductCategory.BANNER)))
        val top = productReader.read(productAppender.append(productDto("채용공고 홈 상단")))
        val bottom = productReader.read(productAppender.append(productDto("채용공고 홈 하단")))
        val removed = manager.addProduct(contract, top)
        manager.addProduct(contract, bottom)
        manager.removeProduct(contract, removed, NOW)
        productManager.delete(bottom, NOW)
        repository.flush()
        entityManager.clear()

        // when
        val products = reader.readProducts(checkNotNull(contract.id))

        // then
        assertEquals(listOf("채용공고 홈 하단"), products.map { it.productName })
        assertEquals(listOf(true), products.map { it.productDeleted })
    }

    @Test
    fun `다른 계약에 붙인 상품은 뺄 수 없다`() {
        // given
        val contract = reader.readForUpdate(appendForm(setOf(AdvertisementProductCategory.BANNER)))
        val other = reader.readForUpdate(appendForm(setOf(AdvertisementProductCategory.BANNER)))
        val product = productReader.read(productAppender.append(productDto("채용공고 홈 상단")))
        val contractProductId = manager.addProduct(other, product)

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) {
            manager.removeProduct(contract, contractProductId, NOW)
        }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND, exception.errorCode)
    }

    private fun appendForm(categories: Set<AdvertisementProductCategory>, companyName: String = "렛츠커리어"): Long =
        appender.appendFromForm(
            AdvertisementContractFormAppendDto(
                userId = null,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                companyName = companyName,
                recruitmentTitle = "브랜드 마케팅 인턴 모집",
                recruitmentUrl = "https://example.com/recruit",
                recruitmentDeadline = LocalDate.of(2026, 12, 20),
                purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
                categories = categories,
                scheduleRequest = " ",
                advertisementRequest = null,
                managerName = "김담당",
                email = "marketing@example.com",
                phoneNumber = "010-1234-5678",
                invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
                creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
            ),
        )

    private fun productDto(name: String) = AdvertisementProductSaveDto(
        category = AdvertisementProductCategory.BANNER,
        position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
        name = name,
        price = null,
    )

    private fun appendContract(job: Job): Long = appender.appendForCompanyPosting(
        AdvertisementContractCompanyPostingAppendDto(
            userId = OWNER_ID,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            targetId = checkNotNull(job.id),
            companyName = job.companyName,
            recruitmentTitle = job.title,
            recruitmentDeadline = null,
        ),
    )

    private fun statusOf(contractId: Long): AdvertisementContractStatus {
        repository.flush()
        entityManager.clear()
        return repository.findById(contractId).get().status
    }

    private fun companyJob(): JobAppendDto = JobAppendDto(
        ownerUserId = OWNER_ID,
        companyName = "뱅크",
        title = "백엔드 개발자",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.EXPERIENCED,
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
    )

    private fun formContract(categories: Set<AdvertisementProductCategory>) = AdvertisementContract(
        source = AdvertisementContractSource.APPLICATION_FORM,
        status = AdvertisementContractStatus.REQUESTED,
        userId = null,
        promotionTarget = AdvertisementContractPromotionTarget.JOB,
        targetId = null,
        companyName = "렛츠커리어",
        recruitmentTitle = "브랜드 마케팅 인턴 모집",
        recruitmentUrl = "https://example.com/recruit",
        recruitmentDeadline = LocalDate.of(2026, 12, 20),
        purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
        categories = categories,
        scheduleRequest = null,
        advertisementRequest = null,
        managerName = "김담당",
        email = "marketing@example.com",
        phoneNumber = "010-1234-5678",
        invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
        creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
    )

    companion object {
        private const val OWNER_ID = 7L
        private val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
    }
}
