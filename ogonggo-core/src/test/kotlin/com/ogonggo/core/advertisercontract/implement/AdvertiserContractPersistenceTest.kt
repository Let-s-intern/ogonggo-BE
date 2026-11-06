package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.error.AdvertiserContractErrorCode
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdvertiserContractReader::class, AdvertiserContractAppender::class, AdvertiserContractManager::class)
internal class AdvertiserContractPersistenceTest @Autowired constructor(
    private val reader: AdvertiserContractReader,
    private val appender: AdvertiserContractAppender,
    private val manager: AdvertiserContractManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `산 자리와 결제일을 저장하고 산 자리는 선언 순서로 다시 읽는다`() {
        // given
        val id = append(
            products = listOf(AdContractProduct.AD_POSTING_MYPAGE_SCRAP, AdContractProduct.BANNER_HOME_TOP),
            paidDate = LocalDate.of(2026, 11, 2),
        )
        entityManager.clear()

        // when
        val contract = reader.read(id)

        // then
        assertEquals(listOf(AdContractProduct.BANNER_HOME_TOP, AdContractProduct.AD_POSTING_MYPAGE_SCRAP), contract.products)
        assertEquals(LocalDate.of(2026, 11, 2), contract.paidDate)
        assertEquals(ADVERTISER_ID, contract.advertiserId)
    }

    @Test
    fun `광고주의 계약은 시작이 늦은 것이 먼저이고 같으면 나중에 등록한 것이 먼저이며 지운 계약은 뺀다`() {
        // given
        val older = append(startAt = START)
        val sameStartFirst = append(startAt = START.plusDays(10))
        val sameStartSecond = append(startAt = START.plusDays(10))
        val deleted = append(startAt = START.plusDays(20))
        append(advertiserId = ADVERTISER_ID + 1)
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // when
        val contracts = reader.readAllByAdvertiser(ADVERTISER_ID)

        // then
        assertEquals(listOf(sameStartSecond, sameStartFirst, older), contracts.map { it.id })
        assertEquals(setOf(ADVERTISER_ID, ADVERTISER_ID + 1), reader.readAllByAdvertisers(listOf(ADVERTISER_ID, ADVERTISER_ID + 1)).keys)
    }

    @Test
    fun `수정은 산 자리를 통째로 바꾸고 지운 계약은 다시 지울 수 있지만 수정·조회에서는 찾을 수 없다`() {
        // given
        val id = append(products = listOf(AdContractProduct.BANNER_HOME_TOP))

        // when
        manager.change(reader.readForUpdate(id), terms(products = listOf(AdContractProduct.AD_POSTING_JOB_LIST)))
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(listOf(AdContractProduct.AD_POSTING_JOB_LIST), reader.read(id).products)
        manager.delete(reader.readForDelete(id), NOW)
        manager.delete(reader.readForDelete(id), NOW.plusDays(1))
        entityManager.flush()
        assertEquals(NOW, reader.readForDelete(id).deletedAt)
        listOf({ reader.read(id) }, { reader.readForUpdate(id) }).forEach { read ->
            val exception = assertThrows(EntityNotFoundException::class.java) { read() }
            assertEquals(AdvertiserContractErrorCode.ADVERTISER_CONTRACT_NOT_FOUND, exception.errorCode)
        }
    }

    private fun append(
        advertiserId: Long = ADVERTISER_ID,
        startAt: LocalDateTime = START,
        products: List<AdContractProduct> = emptyList(),
        paidDate: LocalDate? = null,
    ): Long {
        val contract = appender.append(
            AdvertiserContractAppendDto(advertiserId, terms(startAt = startAt, products = products, paidDate = paidDate)),
        )
        entityManager.flush()
        return checkNotNull(contract.id)
    }

    private fun terms(
        startAt: LocalDateTime = START,
        products: List<AdContractProduct> = emptyList(),
        paidDate: LocalDate? = null,
    ) = AdvertiserContractTerms(
        name = "11월 홈 배너",
        startAt = startAt,
        endAt = startAt.plusMonths(1),
        amount = 1_000_000,
        vatIncluded = true,
        paid = true,
        paidDate = paidDate,
        products = products,
        memo = null,
    )

    private companion object {
        const val ADVERTISER_ID = 10L
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
