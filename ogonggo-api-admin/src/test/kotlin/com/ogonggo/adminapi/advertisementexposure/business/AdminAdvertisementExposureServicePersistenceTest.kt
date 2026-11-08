package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
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
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-advertisement-exposure-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminAdvertisementExposureServicePersistenceTest @Autowired constructor(
    private val service: AdminAdvertisementExposureService,
    private val productAppender: AdvertisementProductAppender,
    private val productReader: AdvertisementProductReader,
    private val productManager: AdvertisementProductManager,
    private val exposureAppender: AdvertisementExposureAppender,
    private val exposureReader: AdvertisementExposureReader,
    transactionManager: PlatformTransactionManager,
) {

    @SpyBean
    private lateinit var spiedProductReader: AdvertisementProductReader

    private val transaction = TransactionTemplate(transactionManager)
    private val repeatableReadTransaction = TransactionTemplate(transactionManager).apply {
        isolationLevel = TransactionDefinition.ISOLATION_REPEATABLE_READ
    }

    @Test
    @Tag("concurrency")
    @DisplayName("노출 snapshot 조회 뒤 상품 삭제가 커밋되면 수정은 상품 없음으로 거부한다")
    fun `노출 snapshot 조회 뒤 상품 삭제가 커밋되면 수정은 상품 없음으로 거부한다`() {
        // given
        val productId = checkNotNull(
            transaction.execute {
                productAppender.append(
                    AdvertisementProductSaveDto(
                        category = AdvertisementProductCategory.BANNER,
                        position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                        name = "A-13 삭제 회귀 테스트",
                        price = 300_000,
                    ),
                )
            },
        )
        val exposureId = checkNotNull(
            transaction.execute {
                exposureAppender.append(
                    AdvertisementExposureAppendDto(
                        productId = productId,
                        contractId = null,
                        content = AdvertisementExposureContent(
                            name = "A-13 삭제 회귀 테스트",
                            imageUrl = "https://example.com/banner.png",
                            linkUrl = "/jobs/5501",
                        ),
                        adLabel = true,
                        period = AdvertisementExposurePeriod.ALWAYS,
                        exposed = true,
                    ),
                )
            },
        )
        val snapshotEstablished = CountDownLatch(1)
        val continueUpdate = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val update = executor.submit<Result<Unit?>> {
            runCatching {
                repeatableReadTransaction.execute {
                    check(exposureReader.readByProduct(productId).any { it.id == exposureId }) {
                        "A-13 트랜잭션이 수정 대상 노출을 조회해야 합니다."
                    }
                    snapshotEstablished.countDown()
                    check(continueUpdate.await(10, TimeUnit.SECONDS)) { "A-13 수정 진행 신호를 받지 못했습니다." }
                    service.updateExposure(updateCommand(exposureId))
                }
            }
        }

        try {
            // when
            assertTrue(snapshotEstablished.await(10, TimeUnit.SECONDS), "A-13 트랜잭션이 노출 조회 snapshot을 만들어야 합니다.")
            transaction.execute {
                productManager.delete(productReader.readForUpdate(productId), NOW)
            }
            continueUpdate.countDown()

            // then
            val exception = update.get(20, TimeUnit.SECONDS).exceptionOrNull()
            assertTrue(exception is EntityNotFoundException, "삭제된 상품 수정은 상품 not found여야 합니다.")
            assertEquals(
                AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND,
                (exception as EntityNotFoundException).errorCode,
            )
        } finally {
            continueUpdate.countDown()
            executor.shutdownNow()
            executor.awaitTermination(20, TimeUnit.SECONDS)
        }
    }

    @Test
    @Tag("concurrency")
    @DisplayName("상품 잠금을 기다린 노출 수정은 그 사이 커밋된 켜짐 상태를 보존한다")
    fun `상품 잠금을 기다린 노출 수정은 그 사이 커밋된 켜짐 상태를 보존한다`() {
        // given
        val productId = checkNotNull(
            transaction.execute {
                productAppender.append(
                    AdvertisementProductSaveDto(
                        category = AdvertisementProductCategory.BANNER,
                        position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                        name = "P-06 교차 필드 잠금 회귀 상품",
                        price = 300_000,
                    ),
                )
            },
        )
        val exposureId = checkNotNull(
            transaction.execute {
                exposureAppender.append(
                    AdvertisementExposureAppendDto(
                        productId = productId,
                        contractId = null,
                        content = AdvertisementExposureContent(
                            name = "P-06 교차 필드 잠금 회귀 노출",
                            imageUrl = "https://example.com/banner.png",
                            linkUrl = "/jobs/5501",
                        ),
                        adLabel = true,
                        period = AdvertisementExposurePeriod.ALWAYS,
                        exposed = true,
                    ),
                )
            },
        )

        val productLocked = CountDownLatch(1)
        val updateReachedProductLock = CountDownLatch(1)
        val updateThread = AtomicReference<Thread>()
        val allowA14Change = CountDownLatch(1)
        val updateThreadName = "p06-cross-field-content-update"
        Mockito.doAnswer { invocation ->
            if (Thread.currentThread().name == updateThreadName) {
                updateThread.set(Thread.currentThread())
                updateReachedProductLock.countDown()
            }
            invocation.callRealMethod()
        }.`when`(spiedProductReader).readForUpdate(productId)

        val lockOwnerExecutor = Executors.newSingleThreadExecutor()
        val updateExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, updateThreadName) }
        val lockOwner = lockOwnerExecutor.submit {
            transaction.execute {
                productReader.readForOrderUpdate(productId)
                productLocked.countDown()
                check(updateReachedProductLock.await(10, TimeUnit.SECONDS)) {
                    "A-13이 노출을 읽은 뒤 상품 잠금을 요청해야 합니다."
                }
                check(allowA14Change.await(10, TimeUnit.SECONDS)) { "A-14 실행 허용 신호를 받지 못했습니다." }
                service.changeExposed(exposureId, false)
            }
        }
        val update = updateExecutor.submit<Result<Unit?>> {
            check(productLocked.await(10, TimeUnit.SECONDS)) { "상품 잠금이 준비되지 않았습니다." }
            runCatching {
                transaction.execute {
                    service.updateExposure(updateCommand(exposureId))
                }
            }
        }

        try {
            // when
            assertTrue(productLocked.await(10, TimeUnit.SECONDS), "A-14 쪽 트랜잭션이 상품 행을 잠가야 합니다.")
            assertTrue(updateReachedProductLock.await(10, TimeUnit.SECONDS), "A-13이 상품 잠금 단계에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(updateThread.get()))
            assertFalse(update.isDone, "A-13 수정은 상품 잠금이 풀리기 전 완료되면 안 됩니다.")

            allowA14Change.countDown()
            lockOwner.get(20, TimeUnit.SECONDS)
            // then
            val updateFailure = update.get(20, TimeUnit.SECONDS).exceptionOrNull()

            assertEquals(null, updateFailure)
            val updatedExposure = exposureReader.readByProduct(productId).single()
            assertFalse(updatedExposure.exposed, "A-13 콘텐츠 수정은 커밋된 A-14 켜짐 상태를 덮어쓰면 안 됩니다.")
            assertEquals("수정 시도", updatedExposure.name)
        } finally {
            allowA14Change.countDown()
            lockOwnerExecutor.shutdownNow()
            updateExecutor.shutdownNow()
            lockOwnerExecutor.awaitTermination(20, TimeUnit.SECONDS)
            updateExecutor.awaitTermination(20, TimeUnit.SECONDS)
            transaction.execute {
                exposureReader.readByProduct(productId).forEach { exposure ->
                    service.deleteExposure(checkNotNull(exposure.id))
                }
                productManager.delete(productReader.readForUpdate(productId), NOW)
            }
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

    private fun updateCommand(exposureId: Long) = UpdateAdminAdvertisementExposureCommand(
        exposureId = exposureId,
        contractId = null,
        name = "수정 시도",
        imageUrl = "https://example.com/updated.png",
        linkUrl = "/jobs/5502",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = null,
        endAt = null,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.parse("2026-11-08T09:00:00")
    }
}
