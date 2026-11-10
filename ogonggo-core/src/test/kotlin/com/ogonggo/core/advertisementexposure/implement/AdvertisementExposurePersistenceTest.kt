package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractQueryRepository
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateDto
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureQueryRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductManager
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductQueryRepository
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertisementExposureAppender::class,
    AdvertisementExposureManager::class,
    AdvertisementExposureReader::class,
    AdvertisementExposureQueryRepository::class,
    AdvertisementProductAppender::class,
    AdvertisementProductManager::class,
    AdvertisementProductReader::class,
    AdvertisementProductQueryRepository::class,
    AdvertisementContractAppender::class,
    AdvertisementContractReader::class,
    AdvertisementContractQueryRepository::class,
)
internal class AdvertisementExposurePersistenceTest @Autowired constructor(
    private val appender: AdvertisementExposureAppender,
    private val manager: AdvertisementExposureManager,
    private val reader: AdvertisementExposureReader,
    private val repository: AdvertisementExposureJpaRepository,
    private val productAppender: AdvertisementProductAppender,
    private val productManager: AdvertisementProductManager,
    private val productReader: AdvertisementProductReader,
    private val contractAppender: AdvertisementContractAppender,
    private val entityManager: EntityManager,
    private val transactionManager: PlatformTransactionManager,
) {

    @SpyBean
    private lateinit var spiedProductReader: AdvertisementProductReader

    @Test
    fun `사용자 화면은 켜져 있고 기간 안인 그 자리 노출만 순서대로 준다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val bottom = appendProduct(AdvertisementProductPosition.JOB_HOME_BOTTOM_BANNER)
        val always = append(top)
        val inPeriod = append(top, AdvertisementExposurePeriod(NOW.minusDays(1), NOW.plusDays(1)))
        append(top, AdvertisementExposurePeriod(NOW.plusSeconds(1), NOW.plusDays(1)))
        append(top, AdvertisementExposurePeriod(NOW.minusDays(1), NOW.minusSeconds(1)))
        append(top, exposed = false)
        val deleted = append(top)
        manager.delete(reader.readForUpdate(deleted), NOW)
        append(bottom)
        flushAndClear()

        // when
        val shown = reader.readShown(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW)

        // then
        assertEquals(listOf(always, inPeriod), shown.map { it.id })
    }

    @Test
    fun `지운 상품 자리의 노출은 사용자 화면에 보이지 않는다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        append(top)
        productManager.delete(productReader.read(top), NOW)
        flushAndClear()

        // when & then
        assertEquals(emptyList<Long>(), reader.readShown(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW).map { it.id })
    }

    @Test
    fun `상품 카테고리와 자리를 바꿔도 기존 노출은 같은 상품을 참조한다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val exposureId = append(productId)
        flushAndClear()

        // when
        val product = productReader.read(productId)
        productManager.update(
            product,
            AdvertisementProductSaveDto(
                category = AdvertisementProductCategory.JOB_POSTING,
                position = null,
                name = "채용공고 광고 상품",
                price = 500_000,
            ),
        )
        flushAndClear()

        // then
        val updatedProduct = productReader.read(productId)
        val existingExposures = reader.readByProduct(productId)
        assertEquals(AdvertisementProductCategory.JOB_POSTING, updatedProduct.category)
        assertEquals(null, updatedProduct.position)
        assertEquals(listOf(exposureId), existingExposures.map { it.id })
        assertEquals(listOf(productId), existingExposures.map { it.productId })
    }

    @Test
    fun `새 노출은 그 자리의 맨 뒤에 두고 위로 아래로 옮기면 순서를 1부터 다시 매긴다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val first = append(top)
        val second = append(top)
        val third = append(top)

        // when
        manager.move(reader.readForMove(third), AdvertisementExposureMoveDirection.UP)
        manager.move(reader.readForMove(first), AdvertisementExposureMoveDirection.UP)
        flushAndClear()

        // then
        val ordered = reader.readByProduct(top)
        assertEquals(listOf(first, third, second), ordered.map { it.id })
        assertEquals(listOf(1, 2, 3), ordered.map { it.displayOrder })
    }

    @Test
    fun `순서 이동은 계약 필터와 무관하게 같은 상품의 전체 미삭제 노출에서 인접 항목과 교환한다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val contractId = appendContract()
        val firstSelfPromotion = append(productId)
        val sponsored = append(
            productId,
            period = AdvertisementExposurePeriod(NOW.minusDays(2), NOW.minusDays(1)),
            exposed = false,
            contractId = contractId,
        )
        val lastSelfPromotion = append(productId)

        // when
        manager.move(reader.readForMove(lastSelfPromotion), AdvertisementExposureMoveDirection.UP)
        flushAndClear()

        // then
        val all = reader.readByProduct(productId)
        assertEquals(listOf(firstSelfPromotion, lastSelfPromotion, sponsored), all.map { it.id })
        assertEquals(listOf(1, 2, 3), all.map { it.displayOrder })
    }

    @Test
    fun `노출 전체 수정은 상품 자리 순서 켜짐을 보존하고 나머지 nullable 값을 교체한다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val contractId = appendContract()
        val exposureId = append(productId, contractId = contractId, exposed = false)
        val before = reader.readByProduct(productId).single()

        // when
        manager.update(
            reader.readForUpdate(exposureId),
            AdvertisementExposureUpdateDto(
                contractId = null,
                content = AdvertisementExposureContent(
                    name = "새 배너",
                    imageUrl = "https://example.com/new.png",
                    mobileImageUrl = "https://example.com/new-mobile.png",
                    linkUrl = "/jobs/5502",
                ),
                adLabel = true,
                period = AdvertisementExposurePeriod(NOW, NOW.plusDays(2)),
            ),
        )
        flushAndClear()

        // then
        val updated = reader.readByProduct(productId).single()
        assertEquals(exposureId, updated.id)
        assertEquals(productId, updated.productId)
        assertEquals(before.displayOrder, updated.displayOrder)
        assertEquals(false, updated.exposed)
        assertEquals(null, updated.contractId)
        assertEquals("새 배너", updated.name)
        assertEquals("https://example.com/new.png", updated.imageUrl)
        assertEquals("https://example.com/new-mobile.png", updated.mobileImageUrl)
        assertEquals("/jobs/5502", updated.linkUrl)
        assertEquals(true, updated.adLabel)
        assertEquals(NOW, updated.startAt)
        assertEquals(NOW.plusDays(2), updated.endAt)
    }

    @Test
    fun `노출 삭제는 소프트 삭제하고 다른 순서는 다시 매기지 않는다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val first = append(productId)
        val deleted = append(productId)
        val last = append(productId)
        productManager.delete(productReader.read(productId), NOW)

        // when
        manager.delete(reader.readForUpdate(deleted), NOW)
        flushAndClear()

        // then
        val live = reader.readByProduct(productId)
        assertEquals(listOf(first, last), live.map { it.id })
        assertEquals(listOf(1, 3), live.map { it.displayOrder })
        assertEquals(NOW, repository.findById(deleted).orElseThrow().deletedAt)
    }

    @Test
    fun `삭제된 상품의 미삭제 노출도 켜고 끌 수 있다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val exposureId = append(productId, exposed = true)
        productManager.delete(productReader.read(productId), NOW)

        // when
        manager.changeExposed(reader.readForUpdate(exposureId), false)
        flushAndClear()

        // then
        assertEquals(false, repository.findById(exposureId).orElseThrow().exposed)
    }

    @Test
    fun `A-13 활성 상품 잠금 조회는 소프트 삭제된 상품을 찾지 못한다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val exposureId = append(productId)
        productManager.delete(productReader.readForUpdate(productId), NOW)
        flushAndClear()

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) {
            reader.readForUpdateWithActiveProduct(exposureId)
        }

        // then
        assertEquals(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `삭제된 상품의 미삭제 노출도 이동하고 삭제할 수 있다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val first = append(productId)
        val second = append(productId)
        val third = append(productId)
        productManager.delete(productReader.read(productId), NOW)

        // when
        manager.move(reader.readForMove(third), AdvertisementExposureMoveDirection.UP)
        manager.delete(reader.readForUpdate(first), NOW)
        flushAndClear()

        // then
        assertEquals(listOf(third, second), reader.readByProduct(productId).map { it.id })
        assertEquals(NOW, repository.findById(first).orElseThrow().deletedAt)
    }

    @Test
    fun `세팅 화면은 상품과 계약으로 거르고 존재하지 않는 양수 계약 ID에는 빈 목록을 준다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val otherProduct = appendProduct(AdvertisementProductPosition.JOB_HOME_BOTTOM_BANNER)
        val contractId = appendContract()
        val otherContractId = appendContract()
        val selfPromotion = append(top)
        val sponsoredFirst = append(top, contractId = contractId)
        val otherContract = append(top, contractId = otherContractId)
        val sponsoredSecond = append(top, contractId = contractId)
        val deletedSponsored = append(top, contractId = contractId)
        val otherProductSponsored = append(otherProduct, contractId = contractId)

        // displayOrder가 같은 행은 id 오름차순으로 유지한다.
        entityManager.find(AdvertisementExposure::class.java, selfPromotion).changeDisplayOrder(2)
        entityManager.find(AdvertisementExposure::class.java, sponsoredFirst).changeDisplayOrder(1)
        entityManager.find(AdvertisementExposure::class.java, otherContract).changeDisplayOrder(1)
        entityManager.find(AdvertisementExposure::class.java, sponsoredSecond).changeDisplayOrder(1)
        entityManager.find(AdvertisementExposure::class.java, deletedSponsored).changeDisplayOrder(0)
        entityManager.find(AdvertisementExposure::class.java, otherProductSponsored).changeDisplayOrder(1)
        manager.delete(reader.readForUpdate(deletedSponsored), NOW)
        flushAndClear()

        // when
        val all = reader.readByProduct(top).map { it.id }
        val sponsored = reader.readByProduct(top, contractId).map { it.id }
        val otherProductSponsors = reader.readByProduct(otherProduct, contractId).map { it.id }
        val missingContract = reader.readByProduct(top, Long.MAX_VALUE).map { it.id }

        // then
        assertEquals(
            listOf(sponsoredFirst, otherContract, sponsoredSecond, selfPromotion),
            all,
        )
        assertEquals(listOf(sponsoredFirst, sponsoredSecond), sponsored)
        assertEquals(listOf(otherProductSponsored), otherProductSponsors)
        assertEquals(emptyList<Long>(), missingContract)
    }

    @Test
    fun `JOB_POSTING 노출은 양수 대상 ID와 종류를 저장하고 Reader 재조회에서도 유지한다`() {
        // given
        val productId = productAppender.append(
            AdvertisementProductSaveDto(
                category = AdvertisementProductCategory.JOB_POSTING,
                position = null,
                name = "채용공고 광고 상품",
                price = 500_000,
            ),
        )
        val targetId = 987_654_321L
        val exposureId = appender.append(
            AdvertisementExposureAppendDto(
                productId = productId,
                contractId = null,
                content = AdvertisementExposureContent(
                    targetType = AdvertisementExposureTargetType.JOB,
                    targetId = targetId,
                ),
                adLabel = true,
                period = AdvertisementExposurePeriod.ALWAYS,
                exposed = true,
            ),
        )
        flushAndClear()

        // when
        val exposure = reader.readByProduct(productId).single()

        // then
        assertEquals(exposureId, exposure.id)
        assertEquals(AdvertisementExposureTargetType.JOB, exposure.targetType)
        assertEquals(targetId, exposure.targetId)
    }

    @Test
    fun `삭제된 상품이어도 미삭제 노출은 관리자 자리 목록에서 기간과 켜짐 상태에 관계없이 보인다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val expiredAndDisabled = append(
            productId,
            period = AdvertisementExposurePeriod(NOW.minusDays(2), NOW.minusDays(1)),
            exposed = false,
        )
        productManager.delete(productReader.read(productId), NOW)
        flushAndClear()

        // when
        val exposures = reader.readByProduct(productId)

        // then
        assertEquals(listOf(expiredAndDisabled), exposures.map { it.id })
    }

    @Test
    @DisplayName("관리자와 공개 노출 목록은 안정된 순서로 페이지를 나누고 전체 건수를 반환한다")
    fun `관리자와 공개 노출 목록은 안정된 순서로 페이지를 나누고 전체 건수를 반환한다`() {
        // given
        val productId = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)
        val first = append(productId)
        val hidden = append(productId, exposed = false)
        val last = append(productId)
        flushAndClear()

        // when
        val adminFirstPage = reader.readByProductPage(productId, contractId = null, page = 0, size = 1)
        val adminSecondPage = reader.readByProductPage(productId, contractId = null, page = 1, size = 1)
        val shownFirstPage = reader.readShownPage(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW, page = 0, size = 1)
        val shownSecondPage = reader.readShownPage(AdvertisementProductPosition.JOB_HOME_TOP_BANNER, NOW, page = 1, size = 1)

        // then
        assertEquals(listOf(first), adminFirstPage.items.map { it.id })
        assertEquals(3L, adminFirstPage.totalElements)
        assertEquals(3, adminFirstPage.totalPages)
        assertEquals(listOf(hidden), adminSecondPage.items.map { it.id })
        assertEquals(listOf(first), shownFirstPage.items.map { it.id })
        assertEquals(2L, shownFirstPage.totalElements)
        assertEquals(2, shownFirstPage.totalPages)
        assertEquals(listOf(last), shownSecondPage.items.map { it.id })
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Tag("concurrency")
    @DisplayName("같은 상품에 동시에 등록하면 두 번째 등록은 상품 행 잠금을 기다린 뒤 순서를 계산한다")
    fun `같은 상품에 동시에 등록해도 잠금 뒤 max displayOrder를 계산한다`() {
        // given
        val transaction = TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
        val productId = checkNotNull(transaction.execute { appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER) })
        val first = checkNotNull(transaction.execute { append(productId) })
        val ownerHasProductLock = CountDownLatch(1)
        val releaseOwner = CountDownLatch(1)
        val waiterReachedLock = CountDownLatch(1)
        val waiterThread = AtomicReference<Thread>()
        val ownerThreadName = "advertisement-exposure-append-owner"
        val waiterThreadName = "advertisement-exposure-append-waiter"
        val ownerExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, ownerThreadName) }
        val waiterExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, waiterThreadName) }
        Mockito.doAnswer { invocation ->
            when (Thread.currentThread().name) {
                ownerThreadName -> invocation.callRealMethod().also {
                    ownerHasProductLock.countDown()
                    check(releaseOwner.await(10, TimeUnit.SECONDS)) { "첫 노출 등록의 잠금 해제 신호가 오지 않았습니다." }
                }
                waiterThreadName -> {
                    waiterThread.set(Thread.currentThread())
                    waiterReachedLock.countDown()
                    invocation.callRealMethod()
                }
                else -> invocation.callRealMethod()
            }
        }.`when`(spiedProductReader).readForUpdate(productId)

        // when
        val firstRegistration = ownerExecutor.submit<Long> {
            checkNotNull(transaction.execute { append(productId) })
        }
        try {
            assertTrue(ownerHasProductLock.await(10, TimeUnit.SECONDS), "첫 등록이 상품 행 잠금을 얻어야 합니다.")
            val secondRegistration = waiterExecutor.submit<Long> {
                checkNotNull(transaction.execute { append(productId) })
            }
            assertTrue(waiterReachedLock.await(10, TimeUnit.SECONDS), "두 번째 등록이 상품 잠금 조회에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(waiterThread.get()))
            assertTrue(!secondRegistration.isDone, "두 번째 등록은 첫 등록의 상품 잠금이 풀리기 전 끝나면 안 됩니다.")

            releaseOwner.countDown()
            firstRegistration.get(20, TimeUnit.SECONDS)
            secondRegistration.get(20, TimeUnit.SECONDS)

            // then
            val exposures = reader.readByProduct(productId)
            assertEquals(3, exposures.size)
            assertEquals(listOf(1, 2, 3), exposures.map { it.displayOrder })
            assertTrue(exposures.any { it.id == first })
        } finally {
            releaseOwner.countDown()
            ownerExecutor.shutdownNow()
            waiterExecutor.shutdownNow()
            ownerExecutor.awaitTermination(20, TimeUnit.SECONDS)
            waiterExecutor.awaitTermination(20, TimeUnit.SECONDS)
            Mockito.reset(spiedProductReader)
            transaction.execute {
                reader.readByProduct(productId).forEach { exposure ->
                    val exposureId = checkNotNull(exposure.id)
                    manager.delete(reader.readForUpdate(exposureId), NOW)
                }
                productManager.delete(productReader.readForUpdate(productId), NOW)
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Tag("concurrency")
    @DisplayName("노출 이동 조회는 등록과 같은 상품 행 잠금이 풀릴 때까지 기다린다")
    fun `이동 조회는 A-12 등록과 같은 상품 행 잠금이 풀릴 때까지 기다린다`() {
        // given
        val transaction = TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
        val productId = checkNotNull(transaction.execute { appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER) })
        val exposureId = checkNotNull(transaction.execute { append(productId) })
        val productLocked = CountDownLatch(1)
        val releaseProduct = CountDownLatch(1)
        val moveStarted = CountDownLatch(1)
        val moveCompleted = CountDownLatch(1)
        val moveLockRequestReached = CountDownLatch(1)
        val moveThread = AtomicReference<Thread>()
        val moveThreadName = "advertisement-exposure-move-waiter"
        val ownerExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "advertisement-exposure-lock-owner") }
        val moveExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, moveThreadName) }
        Mockito.doAnswer { invocation ->
            if (Thread.currentThread().name == moveThreadName) {
                moveThread.set(Thread.currentThread())
                moveLockRequestReached.countDown()
            }
            invocation.callRealMethod()
        }.`when`(spiedProductReader).readForOrderUpdate(productId)

        // when
        val lockOwner = ownerExecutor.submit {
            transaction.execute {
                productReader.readForUpdate(productId)
                productLocked.countDown()
                check(releaseProduct.await(10, TimeUnit.SECONDS)) { "상품 잠금 해제 신호가 오지 않았습니다." }
            }
        }
        val moveReader = moveExecutor.submit {
            check(productLocked.await(10, TimeUnit.SECONDS)) { "상품 잠금이 준비되지 않았습니다." }
            transaction.execute {
                moveStarted.countDown()
                reader.readForMove(exposureId)
                moveCompleted.countDown()
            }
        }

        try {
            assertTrue(productLocked.await(10, TimeUnit.SECONDS), "첫 트랜잭션이 상품 행을 잠가야 합니다.")
            assertTrue(moveStarted.await(10, TimeUnit.SECONDS), "이동 조회가 시작되어야 합니다.")
            assertTrue(moveLockRequestReached.await(10, TimeUnit.SECONDS), "이동 조회가 상품 잠금 조회에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(moveThread.get()))
            assertTrue(moveCompleted.count > 0L, "이동 조회가 상품 잠금 해제 전에 완료되면 안 됩니다.")
            releaseProduct.countDown()
            lockOwner.get(20, TimeUnit.SECONDS)
            moveReader.get(20, TimeUnit.SECONDS)

            // then
            assertTrue(moveCompleted.count == 0L, "상품 잠금 해제 뒤 이동 조회가 완료되어야 합니다.")
        } finally {
            releaseProduct.countDown()
            ownerExecutor.shutdownNow()
            moveExecutor.shutdownNow()
            ownerExecutor.awaitTermination(20, TimeUnit.SECONDS)
            moveExecutor.awaitTermination(20, TimeUnit.SECONDS)
            Mockito.reset(spiedProductReader)
            transaction.execute {
                reader.readByProduct(productId).forEach { exposure ->
                    manager.delete(reader.readForUpdate(checkNotNull(exposure.id)), NOW)
                }
                productManager.delete(productReader.readForUpdate(productId), NOW)
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Tag("concurrency")
    @DisplayName("순서 이동 중인 노출의 삭제는 상품 잠금을 기다려 삭제 상태를 보존한다")
    fun `A-15 순서 이동 중 A-16 삭제는 상품 잠금을 기다려 삭제 상태를 보존한다`() {
        // given
        val transaction = TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
        val productId = checkNotNull(transaction.execute { appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER) })
        val first = checkNotNull(transaction.execute { append(productId) })
        val middle = checkNotNull(transaction.execute { append(productId) })
        val last = checkNotNull(transaction.execute { append(productId) })
        val moveLockedAndLoadedSiblings = CountDownLatch(1)
        val allowMove = CountDownLatch(1)
        val deleteAttempting = CountDownLatch(1)
        val deleteCompleted = CountDownLatch(1)
        val deleteLockRequestReached = CountDownLatch(1)
        val deleteThread = AtomicReference<Thread>()
        val deleteThreadName = "advertisement-exposure-delete-waiter"
        val moveExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "advertisement-exposure-move-owner") }
        val deleteExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, deleteThreadName) }
        Mockito.doAnswer { invocation ->
            if (Thread.currentThread().name == deleteThreadName) {
                deleteThread.set(Thread.currentThread())
                deleteLockRequestReached.countDown()
            }
            invocation.callRealMethod()
        }.`when`(spiedProductReader).readForOrderUpdate(productId)

        // when
        val move = moveExecutor.submit {
            transaction.execute {
                val exposure = reader.readForMove(middle)
                // A-15's initial ordinary read has established a snapshot; also keep the changing sibling managed.
                reader.readByProduct(productId)
                moveLockedAndLoadedSiblings.countDown()
                check(allowMove.await(10, TimeUnit.SECONDS)) { "순서 이동 허용 신호가 오지 않았습니다." }
                manager.move(exposure, AdvertisementExposureMoveDirection.UP)
                repository.flush()
            }
        }
        val delete = deleteExecutor.submit {
            check(moveLockedAndLoadedSiblings.await(10, TimeUnit.SECONDS)) { "순서 이동 상품 잠금이 준비되지 않았습니다." }
            transaction.execute {
                // Establish A-16's ordinary lookup before it tries to follow the product → exposure lock order.
                repository.findByIdAndDeletedAtIsNull(first)
                deleteAttempting.countDown()
                manager.delete(reader.readForUpdate(first), NOW)
            }
            deleteCompleted.countDown()
        }

        try {
            assertTrue(moveLockedAndLoadedSiblings.await(10, TimeUnit.SECONDS), "A-15가 상품을 잠그고 형제를 읽어야 합니다.")
            assertTrue(deleteAttempting.await(10, TimeUnit.SECONDS), "A-16 삭제가 잠금을 요청해야 합니다.")
            assertTrue(deleteLockRequestReached.await(10, TimeUnit.SECONDS), "A-16이 상품 잠금 조회에 도달해야 합니다.")
            awaitH2RowLockWait(checkNotNull(deleteThread.get()))
            assertTrue(deleteCompleted.count > 0L, "A-16은 A-15가 같은 상품 행 잠금을 풀기 전 끝나면 안 됩니다.")
            allowMove.countDown()
            move.get(20, TimeUnit.SECONDS)
            delete.get(20, TimeUnit.SECONDS)

            // then
            assertTrue(deleteCompleted.count == 0L, "A-15 커밋 뒤 A-16 삭제가 완료되어야 합니다.")
            assertEquals(NOW, repository.findById(first).orElseThrow().deletedAt)
            val live = reader.readByProduct(productId)
            assertEquals(listOf(middle, last), live.map { it.id })
            assertEquals(listOf(1, 3), live.map { it.displayOrder })
        } finally {
            allowMove.countDown()
            moveExecutor.shutdownNow()
            deleteExecutor.shutdownNow()
            moveExecutor.awaitTermination(20, TimeUnit.SECONDS)
            deleteExecutor.awaitTermination(20, TimeUnit.SECONDS)
            Mockito.reset(spiedProductReader)
            transaction.execute {
                reader.readByProduct(productId).forEach { exposure ->
                    manager.delete(reader.readForUpdate(checkNotNull(exposure.id)), NOW)
                }
                productManager.delete(productReader.readForUpdate(productId), NOW)
            }
        }
    }

    @Test
    fun `없는 광고주 계약으로는 노출을 만들 수 없다`() {
        // given
        val top = appendProduct(AdvertisementProductPosition.JOB_HOME_TOP_BANNER)

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) { append(top, contractId = 999L) }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND, exception.errorCode)
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

    private fun appendProduct(position: AdvertisementProductPosition): Long = productAppender.append(
        AdvertisementProductSaveDto(
            category = AdvertisementProductCategory.BANNER,
            position = position,
            name = position.desc,
            price = 300_000,
        ),
    )

    private fun append(
        productId: Long,
        period: AdvertisementExposurePeriod = AdvertisementExposurePeriod.ALWAYS,
        exposed: Boolean = true,
        contractId: Long? = null,
    ): Long = appender.append(
        AdvertisementExposureAppendDto(
            productId = productId,
            contractId = contractId,
            content = AdvertisementExposureContent(
                name = "오늘의 공고 홍보",
                imageUrl = "https://example.com/banner.png",
                linkUrl = "/calendar",
            ),
            adLabel = false,
            period = period,
            exposed = exposed,
        ),
    )

    private fun appendContract(): Long = contractAppender.appendFromForm(
        AdvertisementContractFormAppendDto(
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "코드스테이션",
            recruitmentTitle = "10월 기획전",
            recruitmentUrl = "https://example.com/recruit",
            recruitmentDeadline = LocalDate.of(2026, 12, 20),
            purpose = AdvertisementContractPurpose.BRAND_AWARENESS,
            budgetAmount = null,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "010-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.PROVIDE_DIRECTLY,
        ),
    )

    private fun flushAndClear() {
        repository.flush()
        entityManager.clear()
    }

    companion object {
        private val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
    }
}
