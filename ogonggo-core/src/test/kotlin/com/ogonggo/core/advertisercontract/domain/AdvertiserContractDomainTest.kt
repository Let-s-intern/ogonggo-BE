package com.ogonggo.core.advertisercontract.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AdvertiserContractDomainTest {

    @Test
    fun `계약 내용은 이름·메모를 다듬고 결제 전이면 결제일을 지우며 산 자리를 겹치지 않게 선언 순서로 둔다`() {
        // when
        val terms = terms(
            name = "  ",
            memo = " 11월 홈 배너 ",
            paid = false,
            paidDate = LocalDate.of(2026, 11, 1),
            products = listOf(
                AdContractProduct.AD_POSTING_CALENDAR,
                AdContractProduct.BANNER_HOME_TOP,
                AdContractProduct.AD_POSTING_CALENDAR,
            ),
        )

        // then
        assertNull(terms.name)
        assertEquals("11월 홈 배너", terms.memo)
        assertNull(terms.paidDate)
        assertEquals(listOf(AdContractProduct.BANNER_HOME_TOP, AdContractProduct.AD_POSTING_CALENDAR), terms.products)
    }

    @Test
    fun `광고 끝은 시작보다 뒤여야 하고 금액은 0원부터 9,999,999,999원까지다`() {
        assertThrows(IllegalArgumentException::class.java) { terms(startAt = START, endAt = START) }
        assertThrows(IllegalArgumentException::class.java) { terms(amount = -1) }
        assertThrows(IllegalArgumentException::class.java) { terms(amount = 10_000_000_000) }
        assertEquals(9_999_999_999, terms(amount = 9_999_999_999).amount)
    }

    @Test
    fun `진행 중은 시작을 포함하고 끝을 포함하지 않는다`() {
        // given
        val contract = AdvertiserContract(advertiserId = 1, terms = terms())

        // then
        assertFalse(contract.isActive(START.minusSeconds(1)))
        assertTrue(contract.isActive(START))
        assertTrue(contract.isActive(END.minusSeconds(1)))
        assertFalse(contract.isActive(END))
    }

    @Test
    fun `삭제한 계약은 고칠 수 없고 삭제를 반복해도 처음 삭제 일시를 지킨다`() {
        // given
        val contract = AdvertiserContract(advertiserId = 1, terms = terms())
        contract.delete(START)

        // when
        contract.delete(END)

        // then
        assertEquals(START, contract.deletedAt)
        assertThrows(IllegalStateException::class.java) { contract.change(terms(amount = 1)) }
    }

    private fun terms(
        name: String? = "11월 홈 배너",
        startAt: LocalDateTime = START,
        endAt: LocalDateTime = END,
        amount: Long = 1_000_000,
        paid: Boolean = true,
        paidDate: LocalDate? = null,
        products: List<AdContractProduct> = emptyList(),
        memo: String? = null,
    ) = AdvertiserContractTerms(
        name = name,
        startAt = startAt,
        endAt = endAt,
        amount = amount,
        vatIncluded = true,
        paid = paid,
        paidDate = paidDate,
        products = products,
        memo = memo,
    )

    private companion object {
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
        val END: LocalDateTime = LocalDateTime.of(2026, 12, 1, 0, 0)
    }
}
