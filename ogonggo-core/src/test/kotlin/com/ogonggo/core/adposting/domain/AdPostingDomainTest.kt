package com.ogonggo.core.adposting.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AdPostingDomainTest {

    @Test
    fun `노출 기간은 시작을 포함하고 끝을 포함하지 않으며 비어 있는 쪽은 열려 있다`() {
        // given
        val period = AdPostingPeriod(START, END)

        // then
        assertTrue(period.contains(START))
        assertFalse(period.contains(START.minusSeconds(1)))
        assertTrue(period.contains(END.minusSeconds(1)))
        assertFalse(period.contains(END))
        assertTrue(AdPostingPeriod(null, null).contains(START))
        assertTrue(AdPostingPeriod(null, END).contains(START.minusYears(1)))
        assertFalse(AdPostingPeriod(START, null).contains(START.minusSeconds(1)))
    }

    @Test
    fun `노출 끝이 시작보다 뒤가 아니면 기간을 만들 수 없다`() {
        assertFalse(AdPostingPeriod.isValid(START, START))
        assertThrows(IllegalArgumentException::class.java) { AdPostingPeriod(START, START.minusDays(1)) }
    }

    @Test
    fun `꺼져 있으면 기간 안이어도 노출 중이 아니다`() {
        // given
        val adPosting = adPosting(enabled = false)

        // then
        assertFalse(adPosting.isLive(START))
        adPosting.change(ADVERTISER_ID, null, AdPostingPeriod(START, END), enabled = true)
        assertTrue(adPosting.isLive(START))
        assertFalse(adPosting.isLive(END))
    }

    @Test
    fun `대상 종류는 자리가 정해 부트캠프 목록만 부트캠프다`() {
        AdPostingPlacement.entries.forEach { placement ->
            val expected = if (placement == AdPostingPlacement.BOOTCAMP_LIST) AdPostingTargetType.BOOTCAMP else AdPostingTargetType.JOB
            assertEquals(expected, adPosting(placement = placement).targetType)
        }
    }

    @Test
    fun `삭제한 광고 공고는 고칠 수 없고 다시 지워도 처음 삭제 일시를 지킨다`() {
        // given
        val adPosting = adPosting()

        // when
        adPosting.delete(START)
        adPosting.delete(END)

        // then
        assertEquals(START, adPosting.deletedAt)
        assertThrows(IllegalStateException::class.java) {
            adPosting.change(ADVERTISER_ID, null, AdPostingPeriod(null, null), enabled = true)
        }
    }

    private fun adPosting(
        placement: AdPostingPlacement = AdPostingPlacement.JOB_LIST,
        enabled: Boolean = true,
    ) = AdPosting(
        placement = placement,
        targetId = 1,
        advertiserId = ADVERTISER_ID,
        contractId = null,
        period = AdPostingPeriod(START, END),
        enabled = enabled,
        displayOrder = 1,
    )

    private companion object {
        const val ADVERTISER_ID = 10L
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
        val END: LocalDateTime = LocalDateTime.of(2026, 12, 1, 0, 0)
    }
}
