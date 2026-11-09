package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractService
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Tag("concurrency")
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-advertisement-product-update-delete-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminAdvertisementProductConcurrencyTest @Autowired constructor(
    private val productService: AdminAdvertisementProductService,
    private val contractService: AdminAdvertisementContractService,
    private val productAppender: AdvertisementProductAppender,
    private val productReader: AdvertisementProductReader,
    private val contractAppender: AdvertisementContractAppender,
    private val contractReader: AdvertisementContractReader,
    private val transactionManager: PlatformTransactionManager,
) {

    @SpyBean
    private lateinit var spiedProductReader: AdvertisementProductReader

    @Test
    @DisplayName("수정이 상품 잠금을 먼저 얻으면 대기한 삭제는 수정 값이 반영된 뒤 상품을 삭제한다")
    fun `수정이 먼저 상품 잠금을 얻으면 수정 값이 반영된 뒤 삭제 상태를 유지한다`() {
        // given
        val productId = appendProduct()
        val contractId = appendContract()
        val updatePrepared = CountDownLatch(1)
        val allowUpdateCommit = CountDownLatch(1)
        val deleteStarted = CountDownLatch(1)
        val deleteCompleted = CountDownLatch(1)
        val deleteLockRequestReached = CountDownLatch(1)
        val deleteThread = AtomicReference<Thread>()
        val deleteThreadName = "advertisement-product-delete-waiter"
        val updateExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "advertisement-product-update-owner") }
        val deleteExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, deleteThreadName) }
        val command = updatedCommand()
        Mockito.doAnswer { invocation ->
            if (Thread.currentThread().name == deleteThreadName) {
                deleteThread.set(Thread.currentThread())
                deleteLockRequestReached.countDown()
            }
            invocation.callRealMethod()
        }.`when`(spiedProductReader).readForUpdate(productId)

        // when
        val update = updateExecutor.submit {
            newTransaction().execute {
                productService.replaceProduct(productId, command)
                updatePrepared.countDown()
                check(allowUpdateCommit.await(10, TimeUnit.SECONDS)) { "상품 수정 커밋 신호를 받지 못했습니다." }
            }
        }
        val delete = deleteExecutor.submit {
            check(updatePrepared.await(10, TimeUnit.SECONDS)) { "상품 수정 트랜잭션이 준비되어야 합니다." }
            deleteStarted.countDown()
            productService.deleteProduct(productId).also { deleteCompleted.countDown() }
        }

        try {
            assertTrue(updatePrepared.await(10, TimeUnit.SECONDS), "상품 수정이 잠금 안에서 완료되어야 합니다.")
            assertTrue(deleteStarted.await(10, TimeUnit.SECONDS), "상품 삭제 요청이 시작되어야 합니다.")
            assertTrue(deleteLockRequestReached.await(10, TimeUnit.SECONDS), "삭제가 상품 잠금 조회에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(deleteThread.get()))
            assertFalse(deleteCompleted.count == 0L, "상품 삭제가 수정 트랜잭션의 잠금 해제 전에 완료되면 안 됩니다.")
            allowUpdateCommit.countDown()
            update.get(20, TimeUnit.SECONDS)
            delete.get(20, TimeUnit.SECONDS)

            // then
            val product = checkNotNull(productReader.readAllByIds(listOf(productId))[productId])
            assertEquals(command.category, product.category)
            assertEquals(command.position, product.position)
            assertEquals(command.name, product.name)
            assertEquals(command.price, product.price)
            assertTrue(product.deletedAt != null, "수정 뒤 삭제된 상품이어야 합니다.")
            assertHiddenFromListsAndCannotBeLinked(productId, contractId)
        } finally {
            allowUpdateCommit.countDown()
            updateExecutor.shutdownNow()
            deleteExecutor.shutdownNow()
            updateExecutor.awaitTermination(20, TimeUnit.SECONDS)
            deleteExecutor.awaitTermination(20, TimeUnit.SECONDS)
            Mockito.reset(spiedProductReader)
        }
    }

    @Test
    @DisplayName("삭제가 상품 잠금을 먼저 얻으면 대기한 수정은 상품 없음으로 거부된다")
    fun `삭제가 먼저 상품 잠금을 얻으면 대기한 수정은 없는 상품으로 거부된다`() {
        // given
        val productId = appendProduct()
        val contractId = appendContract()
        val deletePrepared = CountDownLatch(1)
        val allowDeleteCommit = CountDownLatch(1)
        val updateStarted = CountDownLatch(1)
        val updateCompleted = CountDownLatch(1)
        val updateLockRequestReached = CountDownLatch(1)
        val updateThread = AtomicReference<Thread>()
        val updateThreadName = "advertisement-product-update-waiter"
        val deleteExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "advertisement-product-delete-owner") }
        val updateExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, updateThreadName) }
        Mockito.doAnswer { invocation ->
            if (Thread.currentThread().name == updateThreadName) {
                updateThread.set(Thread.currentThread())
                updateLockRequestReached.countDown()
            }
            invocation.callRealMethod()
        }.`when`(spiedProductReader).readForUpdate(productId)

        // when
        val delete = deleteExecutor.submit {
            newTransaction().execute {
                productService.deleteProduct(productId)
                deletePrepared.countDown()
                check(allowDeleteCommit.await(10, TimeUnit.SECONDS)) { "상품 삭제 커밋 신호를 받지 못했습니다." }
            }
        }
        val update = updateExecutor.submit<Result<Unit>> {
            check(deletePrepared.await(10, TimeUnit.SECONDS)) { "상품 삭제 트랜잭션이 준비되어야 합니다." }
            updateStarted.countDown()
            runCatching { productService.replaceProduct(productId, updatedCommand()) }
                .also { updateCompleted.countDown() }
        }

        try {
            assertTrue(deletePrepared.await(10, TimeUnit.SECONDS), "상품 삭제가 잠금 안에서 완료되어야 합니다.")
            assertTrue(updateStarted.await(10, TimeUnit.SECONDS), "상품 수정 요청이 시작되어야 합니다.")
            assertTrue(updateLockRequestReached.await(10, TimeUnit.SECONDS), "수정이 상품 잠금 조회에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(updateThread.get()))
            assertFalse(updateCompleted.count == 0L, "상품 수정이 삭제 트랜잭션의 잠금 해제 전에 완료되면 안 됩니다.")
            allowDeleteCommit.countDown()
            delete.get(20, TimeUnit.SECONDS)

            // then
            val error = update.get(20, TimeUnit.SECONDS).exceptionOrNull()
            assertTrue(error is EntityNotFoundException)
            assertEquals(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND, (error as EntityNotFoundException).errorCode)
            val product = checkNotNull(productReader.readAllByIds(listOf(productId))[productId])
            assertEquals("잠금 경합 테스트 상품", product.name)
            assertTrue(product.deletedAt != null, "삭제가 먼저 커밋되면 상품은 삭제 상태여야 합니다.")
            assertHiddenFromListsAndCannotBeLinked(productId, contractId)
        } finally {
            allowDeleteCommit.countDown()
            deleteExecutor.shutdownNow()
            updateExecutor.shutdownNow()
            deleteExecutor.awaitTermination(20, TimeUnit.SECONDS)
            updateExecutor.awaitTermination(20, TimeUnit.SECONDS)
            Mockito.reset(spiedProductReader)
        }
    }

    private fun awaitH2RowLockWait(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var lastStack: Array<StackTraceElement> = emptyArray()
        while (System.nanoTime() < deadline) {
            lastStack = thread.stackTrace
            val isWaiting = thread.state in setOf(Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.BLOCKED)
            if (isWaiting && lastStack.any { it.className.startsWith("org.h2.") }) return
            Thread.sleep(10)
        }
        throw AssertionError(
            "대기 스레드가 H2 잠금 대기에 도달하지 않았습니다. state=${thread.state}, stack=${lastStack.joinToString(" <- ")}",
        )
    }

    private fun assertHiddenFromListsAndCannotBeLinked(productId: Long, contractId: Long) {
        assertFalse(productService.getProducts(0, 100).items.any { it.id == productId }, "관리자 상품 목록에 삭제 상품이 없어야 합니다.")
        assertFalse(productReader.readAll().any { it.id == productId }, "사용자 상품 목록의 공통 reader에서 삭제 상품이 없어야 합니다.")

        val error = runCatching { contractService.addProduct(contractId, productId) }.exceptionOrNull()
        assertTrue(error is EntityNotFoundException)
        assertEquals(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND, (error as EntityNotFoundException).errorCode)
        assertTrue(contractReader.readProducts(contractId).isEmpty(), "삭제 상품의 새 계약 연결이 생기면 안 됩니다.")
    }

    private fun newTransaction() = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    private fun appendProduct(): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.BANNER,
            position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
            name = "잠금 경합 테스트 상품",
            price = 500_000,
        ),
    )

    private fun updatedCommand() = AdminAdvertisementProductSaveCommand(
        category = AdvertisementProductCategory.JOB_POSTING,
        position = null,
        name = "수정된 잠금 경합 테스트 상품",
        price = 750_000,
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
            budgetRange = null,
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
}
