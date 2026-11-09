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
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductQueryRepository
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
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
    AdvertisementProductQueryRepository::class,
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
    fun `신청 폼 계약은 사업자 번호를 저장하고 비즈니스 공고 계약은 비워 둔다`() {
        // given
        val formId = appendForm(
            setOf(AdvertisementProductCategory.KAKAO_CRM),
            businessNumberType = CompanyBusinessNumberType.CORPORATE_REGISTRATION,
            businessNumber = "1101111234567",
        )
        val postingId = appendContract(jobAppender.append(companyJob()))
        repository.flush()
        entityManager.clear()

        // when
        val form = repository.findById(formId).get()
        val posting = repository.findById(postingId).get()

        // then
        assertEquals(CompanyBusinessNumberType.CORPORATE_REGISTRATION, form.businessNumberType)
        assertEquals("1101111234567", form.businessNumber)
        assertEquals(setOf(AdvertisementProductCategory.KAKAO_CRM), form.categories)
        assertEquals(null, posting.businessNumberType)
        assertEquals(null, posting.businessNumber)
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
    fun `협의 금액은 0 이상으로 저장하고 null로 해제한다`() {
        // given
        val contractId = appendForm(setOf(AdvertisementProductCategory.BANNER))

        // when
        manager.changePaymentAmount(reader.readForUpdate(contractId), 0)
        repository.flush()
        entityManager.clear()

        // then
        assertEquals(0, reader.read(contractId).paymentAmount)

        // when
        manager.changePaymentAmount(reader.readForUpdate(contractId), null)
        repository.flush()
        entityManager.clear()

        // then
        assertEquals(null, reader.read(contractId).paymentAmount)
    }

    @Test
    fun `같은 상품을 계약에 여러 번 연결하면 각 연결에 별도 ID를 만든다`() {
        // given
        val contract = reader.readForUpdate(appendForm(setOf(AdvertisementProductCategory.BANNER)))
        val product = productReader.read(productAppender.append(productDto("채용공고 홈 상단")))

        // when
        val firstId = manager.addProduct(contract, product)
        val secondId = manager.addProduct(contract, product)
        repository.flush()
        entityManager.clear()

        // then
        assertNotEquals(firstId, secondId)
        assertEquals(listOf(firstId, secondId), reader.readProducts(checkNotNull(contract.id)).map { it.contractProductId })
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
    fun `계약 목록은 페이지 카테고리를 일괄 조회하고 계약별 결과를 유지한다`() {
        // given
        val bannerId = appendForm(setOf(AdvertisementProductCategory.BANNER))
        val kakaoId = appendForm(setOf(AdvertisementProductCategory.KAKAO_1, AdvertisementProductCategory.INSTAGRAM_1))
        repository.flush()
        entityManager.clear()
        val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        // when
        val result = reader.readPage(AdvertisementContractSearchCondition(), page = 0, size = 10)
        val categoriesByContractId = result.advertisementContracts.associate {
            checkNotNull(it.id) to it.categories.toSet()
        }

        // then
        assertEquals(
            mapOf(
                kakaoId to setOf(AdvertisementProductCategory.KAKAO_1, AdvertisementProductCategory.INSTAGRAM_1),
                bannerId to setOf(AdvertisementProductCategory.BANNER),
            ),
            categoriesByContractId,
        )
        assertEquals(2, statistics.prepareStatementCount, "페이지 쿼리와 페이지 내 카테고리 일괄 조회만 실행해야 한다")
    }

    @Test
    fun `계약 목록은 필터 결과 전체를 기준으로 페이지 개수를 준다`() {
        // given
        val bannerIds = listOf(
            appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "첫 번째 배너 회사"),
            appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "두 번째 배너 회사"),
        )
        appendForm(setOf(AdvertisementProductCategory.KAKAO_1), companyName = "카카오 회사")
        repository.flush()
        entityManager.clear()
        val condition = AdvertisementContractSearchCondition(
            source = AdvertisementContractSource.APPLICATION_FORM,
            category = AdvertisementProductCategory.BANNER,
        )

        // when
        val allMatches = reader.readPage(condition, page = 0, size = 10)
        val firstPage = reader.readPage(condition, page = 0, size = 1)

        // then
        assertEquals(bannerIds.sortedDescending(), allMatches.advertisementContracts.map { it.id })
        assertEquals(2L, allMatches.totalElements)
        assertEquals(allMatches.totalElements, firstPage.totalElements)
        assertEquals(allMatches.advertisementContracts.size, firstPage.totalPages)
        assertEquals(bannerIds.sortedDescending().take(1), firstPage.advertisementContracts.map { it.id })
    }

    @Test
    fun `계약 목록은 status 중 하나와 AND 필터를 적용하고 생성일 양 끝을 포함한다`() {
        // given
        val firstDay = appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "LetsCareer")
        val lastDay = appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "LetsCareer Labs")
        val wrongCompany = appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "Other Company")
        val afterRange = appendForm(setOf(AdvertisementProductCategory.BANNER), companyName = "LetsCareer")
        manager.changeStatus(reader.readForUpdate(lastDay), AdvertisementContractStatus.NEGOTIATING)
        repository.flush()
        setCreatedAt(firstDay, LocalDateTime.of(2026, 11, 1, 0, 0))
        setCreatedAt(lastDay, LocalDateTime.of(2026, 11, 30, 23, 59, 59, 999_000_000))
        setCreatedAt(wrongCompany, LocalDateTime.of(2026, 11, 15, 12, 0))
        setCreatedAt(afterRange, LocalDateTime.of(2026, 12, 1, 0, 0))
        entityManager.clear()

        // when
        val result = reader.readPage(
            condition = AdvertisementContractSearchCondition(
                source = AdvertisementContractSource.APPLICATION_FORM,
                statuses = setOf(AdvertisementContractStatus.REQUESTED, AdvertisementContractStatus.NEGOTIATING),
                category = AdvertisementProductCategory.BANNER,
                companyName = "  letscareer  ",
                createdFrom = LocalDate.of(2026, 11, 1),
                createdTo = LocalDate.of(2026, 11, 30),
            ),
            page = 0,
            size = 10,
        )

        // then
        assertEquals(listOf(lastDay, firstDay), result.advertisementContracts.map { it.id })
    }

    @Test
    fun `회원의 신청 내역은 그 회원이 폼으로 낸 신청만 최근 신청부터 준다`() {
        // given
        val first = appendForm(setOf(AdvertisementProductCategory.BANNER), userId = OWNER_ID)
        appendForm(setOf(AdvertisementProductCategory.BANNER), userId = OTHER_USER_ID)
        appendForm(setOf(AdvertisementProductCategory.BANNER))
        val second = appendForm(setOf(AdvertisementProductCategory.KAKAO_1), userId = OWNER_ID)
        appendContract(jobAppender.append(companyJob()))
        repository.flush()
        entityManager.clear()
        val condition = AdvertisementContractSearchCondition(
            source = AdvertisementContractSource.APPLICATION_FORM,
            userId = OWNER_ID,
        )

        // when
        val all = reader.readPage(condition, page = 0, size = 10)
        val secondPage = reader.readPage(condition, page = 1, size = 1)

        // then
        assertEquals(listOf(second, first), all.advertisementContracts.map { it.id })
        assertEquals(2L, all.totalElements)
        assertEquals(listOf(first), secondPage.advertisementContracts.map { it.id })
        assertEquals(2, secondPage.totalPages)
    }

    @Test
    fun `회원 신청 단건은 그 회원이 폼으로 낸 신청만 찾는다`() {
        // given
        val mine = appendForm(setOf(AdvertisementProductCategory.BANNER), userId = OWNER_ID)
        val others = appendForm(setOf(AdvertisementProductCategory.BANNER), userId = OTHER_USER_ID)
        val guest = appendForm(setOf(AdvertisementProductCategory.BANNER))
        val posting = appendContract(jobAppender.append(companyJob()))
        repository.flush()
        entityManager.clear()

        // when & then
        assertEquals(mine, reader.readApplicationOf(OWNER_ID, mine).id)
        listOf(others, guest, posting, Long.MAX_VALUE).forEach { contractId ->
            val exception = assertThrows(EntityNotFoundException::class.java) {
                reader.readApplicationOf(OWNER_ID, contractId)
            }
            assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND, exception.errorCode)
        }
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
        assertEquals(listOf(AdvertisementProductPosition.JOB_HOME_TOP_BANNER), products.map { it.position })
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

    @Test
    fun `이미 제거한 계약 상품은 다시 제거할 수 없다`() {
        // given
        val contract = reader.readForUpdate(appendForm(setOf(AdvertisementProductCategory.BANNER)))
        val product = productReader.read(productAppender.append(productDto("채용공고 홈 상단")))
        val contractProductId = manager.addProduct(contract, product)
        manager.removeProduct(contract, contractProductId, NOW)

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) {
            manager.removeProduct(contract, contractProductId, NOW.plusMinutes(1))
        }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND, exception.errorCode)
    }

    private fun appendForm(
        categories: Set<AdvertisementProductCategory>,
        companyName: String = "렛츠커리어",
        userId: Long? = null,
        businessNumberType: CompanyBusinessNumberType? = null,
        businessNumber: String? = null,
    ): Long =
        appender.appendFromForm(
            AdvertisementContractFormAppendDto(
                userId = userId,
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
                businessNumberType = businessNumberType,
                businessNumber = businessNumber,
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

    private fun setCreatedAt(contractId: Long, createdAt: LocalDateTime) {
        entityManager.createNativeQuery("update advertisement_contracts set created_at = :createdAt where id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", contractId)
            .executeUpdate()
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
        businessNumberType = null,
        businessNumber = null,
        invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
        creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
    )

    companion object {
        private const val OWNER_ID = 7L
        private const val OTHER_USER_ID = 8L
        private val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
    }
}
