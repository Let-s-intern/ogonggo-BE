package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractService
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("concurrency")
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-advertisement-product-contract-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminAdvertisementProductContractConcurrencyTest @Autowired constructor(
    private val productService: AdminAdvertisementProductService,
    private val contractService: AdminAdvertisementContractService,
    private val productAppender: AdvertisementProductAppender,
    private val productReader: AdvertisementProductReader,
    private val contractAppender: AdvertisementContractAppender,
    private val contractReader: AdvertisementContractReader,
    private val exposureAppender: AdvertisementExposureAppender,
    private val exposureReader: AdvertisementExposureReader,
    private val transactionManager: PlatformTransactionManager,
) {

    @Test
    fun `삭제가 먼저 상품 잠금을 얻으면 뒤의 계약 상품 연결은 거부된다`() {
        // given
        val productId = appendProduct()
        val contractId = appendContract()
        val productLocked = CountDownLatch(1)
        val allowDelete = CountDownLatch(1)
        val linkStarted = CountDownLatch(1)
        val linkCompleted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val delete = executor.submit {
            newTransaction().execute {
                productReader.readForUpdate(productId)
                productLocked.countDown()
                check(allowDelete.await(10, TimeUnit.SECONDS)) { "상품 삭제 진행 신호를 받지 못했습니다." }
                productService.deleteProduct(productId)
            }
        }
        val link = executor.submit<Result<Long>> {
            check(productLocked.await(10, TimeUnit.SECONDS)) { "삭제 트랜잭션이 상품 잠금을 얻어야 합니다." }
            linkStarted.countDown()
            runCatching { contractService.addProduct(contractId, productId) }
                .also { linkCompleted.countDown() }
        }

        try {
            assertTrue(productLocked.await(10, TimeUnit.SECONDS), "삭제 트랜잭션이 상품 행을 잠가야 합니다.")
            assertTrue(linkStarted.await(10, TimeUnit.SECONDS), "상품 연결 요청이 시작되어야 합니다.")
            assertFalse(linkCompleted.await(300, TimeUnit.MILLISECONDS), "상품 연결은 삭제 트랜잭션의 잠금 해제 전 완료되면 안 됩니다.")
            allowDelete.countDown()
            delete.get(20, TimeUnit.SECONDS)

            // then
            val error = link.get(20, TimeUnit.SECONDS).exceptionOrNull()
            assertTrue(error is EntityNotFoundException)
            assertEquals(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND, (error as EntityNotFoundException).errorCode)
            assertTrue(contractReader.readProducts(contractId).isEmpty())
        } finally {
            allowDelete.countDown()
            executor.shutdownNow()
            executor.awaitTermination(20, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `상품 연결이 먼저 상품 잠금을 얻으면 뒤의 삭제 뒤에도 기존 연결을 보존한다`() {
        // given
        val productId = appendProduct()
        val contractId = appendContract()
        val exposureId = appendExposure(productId)
        val linkCreated = CountDownLatch(1)
        val allowLinkCommit = CountDownLatch(1)
        val deleteStarted = CountDownLatch(1)
        val deleteCompleted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val link = executor.submit {
            newTransaction().execute {
                contractReader.readForUpdate(contractId)
                productReader.readForUpdate(productId)
                contractService.addProduct(contractId, productId)
                linkCreated.countDown()
                check(allowLinkCommit.await(10, TimeUnit.SECONDS)) { "상품 연결 커밋 신호를 받지 못했습니다." }
            }
        }
        val delete = executor.submit {
            check(linkCreated.await(10, TimeUnit.SECONDS)) { "상품 연결 트랜잭션이 상품 행을 잠가야 합니다." }
            deleteStarted.countDown()
            productService.deleteProduct(productId)
                .also { deleteCompleted.countDown() }
        }

        try {
            assertTrue(linkCreated.await(10, TimeUnit.SECONDS), "상품 연결 행이 잠금 안에서 생성되어야 합니다.")
            assertTrue(deleteStarted.await(10, TimeUnit.SECONDS), "상품 삭제 요청이 시작되어야 합니다.")
            assertFalse(deleteCompleted.await(300, TimeUnit.MILLISECONDS), "상품 삭제는 연결 트랜잭션의 잠금 해제 전 완료되면 안 됩니다.")
            allowLinkCommit.countDown()
            link.get(20, TimeUnit.SECONDS)
            delete.get(20, TimeUnit.SECONDS)

            // then
            val linkedProduct = contractReader.readProducts(contractId).single()
            assertEquals(productId, linkedProduct.productId)
            assertEquals(true, linkedProduct.productDeleted)
            assertEquals(listOf(exposureId), exposureReader.readByProduct(productId).map { it.id })
            val missingProduct = assertThrows(EntityNotFoundException::class.java) { productReader.read(productId) }
            assertEquals(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND, missingProduct.errorCode)
        } finally {
            allowLinkCommit.countDown()
            executor.shutdownNow()
            executor.awaitTermination(20, TimeUnit.SECONDS)
        }
    }

    private fun newTransaction() = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    private fun appendProduct(): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.BANNER,
            position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
            name = "상품 잠금 경합 테스트",
            price = null,
        ),
    )

    private fun appendContract(): Long = contractAppender.appendFromForm(
        AdvertisementContractFormAppendDto(
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "잠금 경합 테스트",
            recruitmentTitle = "백엔드 개발자 모집",
            recruitmentUrl = "https://example.com/recruit",
            recruitmentDeadline = LocalDate.of(2026, 12, 20),
            purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "담당자",
            email = "test@example.com",
            phoneNumber = "010-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        ),
    )

    private fun appendExposure(productId: Long): Long = checkNotNull(newTransaction().execute {
        exposureAppender.append(
            AdvertisementExposureAppendDto(
                productId = productId,
                contractId = null,
                content = AdvertisementExposureContent(
                    name = "잠금 경합 테스트 배너",
                    imageUrl = "https://example.com/banner.png",
                    linkUrl = "https://example.com/jobs",
                ),
                adLabel = true,
                period = AdvertisementExposurePeriod.ALWAYS,
                exposed = true,
            ),
        )
    })
}
