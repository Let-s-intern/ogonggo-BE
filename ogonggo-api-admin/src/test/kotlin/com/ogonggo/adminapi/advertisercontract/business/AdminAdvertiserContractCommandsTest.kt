package com.ogonggo.adminapi.advertisercontract.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class AdminAdvertiserContractCommandsTest {

    @Test
    fun `등록 본문은 화면 문구로 처음 걸린 칸 하나만 알린다`() {
        assertMessage("name: 50자까지 보낼 수 있습니다.", create(name = "가".repeat(51), amount = null))
        assertMessage("startAt: YYYY-MM-DDTHH:mm:ss(KST) 로 보내 주세요.", create(startAt = null))
        assertMessage("endAt: 광고 끝은 시작보다 뒤여야 합니다.", create(endAt = START))
        assertMessage("amount: 0 이상의 정수(원)로 보내 주세요.", create(amount = 10_000_000_000))
        assertMessage("amount: 0 이상의 정수(원)로 보내 주세요.", create(amount = -1))
        assertMessage("vatIncluded: 참·거짓 값을 보내 주세요.", create(vatIncluded = null))
        assertMessage("paid: 참·거짓 값을 보내 주세요.", create(paid = null))
        assertMessage(
            "products: BANNER_HOME_TOP, BANNER_HOME_BOTTOM, BANNER_JOB_DETAIL_SIDE, BANNER_BOOTCAMP_TOP, " +
                "BANNER_BOOTCAMP_BOTTOM, BANNER_BOOTCAMP_DETAIL_SIDE, AD_POSTING_JOB_LIST, AD_POSTING_JOB_PICK, " +
                "AD_POSTING_BOOTCAMP_LIST, AD_POSTING_JOB_DETAIL_SIDE, AD_POSTING_CALENDAR, AD_POSTING_MYPAGE_SCRAP " +
                "가운데 겹치지 않게 보내 주세요.",
            create(products = listOf(AdContractProduct.BANNER_HOME_TOP, AdContractProduct.BANNER_HOME_TOP)),
        )
        assertMessage("memo: 500자까지 보낼 수 있습니다.", create(memo = "가".repeat(501)))
    }

    @Test
    fun `산 자리를 보내지 않으면 빈 목록이다`() {
        assertEquals(emptyList<AdContractProduct>(), create(products = null).toDraft().toTerms().products)
    }

    private fun assertMessage(message: String, command: AdminAdvertiserContractCreateCommand) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) { command.toDraft().toTerms() }
        assertEquals(message, exception.message)
    }

    @Test
    fun `수정은 저장된 값에 보낸 칸을 얹어 보며 null을 보낸 칸은 지운다`() {
        // given
        val stored = Mockito.mock(AdvertiserContract::class.java)
        Mockito.`when`(stored.name).thenReturn("11월 홈 배너")
        Mockito.`when`(stored.startAt).thenReturn(START)
        Mockito.`when`(stored.endAt).thenReturn(START.plusMonths(1))
        Mockito.`when`(stored.amount).thenReturn(1_000_000)
        Mockito.`when`(stored.vatIncluded).thenReturn(false)
        Mockito.`when`(stored.paid).thenReturn(true)
        Mockito.`when`(stored.paidDate).thenReturn(LocalDate.of(2026, 11, 2))
        Mockito.`when`(stored.products).thenReturn(mutableListOf(AdContractProduct.BANNER_HOME_TOP))
        Mockito.`when`(stored.memo).thenReturn("메모")

        // when
        val terms = AdminAdvertiserContractUpdateCommand(name = Optional.empty(), endAt = START.plusDays(3))
            .mergeInto(stored)
            .toTerms()

        // then
        assertNull(terms.name)
        assertEquals(START.plusDays(3), terms.endAt)
        assertEquals(false, terms.vatIncluded)
        assertEquals(LocalDate.of(2026, 11, 2), terms.paidDate)
        assertEquals(listOf(AdContractProduct.BANNER_HOME_TOP), terms.products)
        assertEquals("메모", terms.memo)
    }

    @Test
    fun `수정 검증은 저장된 값과 합친 결과로 한다`() {
        // given
        val stored = Mockito.mock(AdvertiserContract::class.java)
        Mockito.`when`(stored.startAt).thenReturn(START)
        Mockito.`when`(stored.endAt).thenReturn(START.plusMonths(1))
        Mockito.`when`(stored.amount).thenReturn(1_000_000)
        Mockito.`when`(stored.vatIncluded).thenReturn(true)
        Mockito.`when`(stored.paid).thenReturn(false)
        Mockito.`when`(stored.products).thenReturn(mutableListOf())

        // when
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            AdminAdvertiserContractUpdateCommand(startAt = START.plusMonths(2)).mergeInto(stored).toTerms()
        }

        // then
        assertEquals("endAt: 광고 끝은 시작보다 뒤여야 합니다.", exception.message)
    }

    private fun create(
        name: String? = "11월 홈 배너",
        startAt: LocalDateTime? = START,
        endAt: LocalDateTime? = START.plusMonths(1),
        amount: Long? = 1_000_000,
        vatIncluded: Boolean? = true,
        paid: Boolean? = true,
        paidDate: LocalDate? = null,
        products: List<AdContractProduct?>? = listOf(AdContractProduct.BANNER_HOME_TOP),
        memo: String? = null,
    ) = AdminAdvertiserContractCreateCommand(
        name = name,
        startAt = startAt,
        endAt = endAt,
        amount = amount,
        vatIncluded = vatIncluded,
        paid = paid,
        paidDate = paidDate,
        products = products,
        memo = memo,
    )

    private companion object {
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
