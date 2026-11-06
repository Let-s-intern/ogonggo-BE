package com.ogonggo.adminapi.advertisercontract.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AdminContractChannelFormatsTest {

    @Test
    fun `계약의 채널 게재 유형은 있는 유형을 겹치지 않게 받고 산 자리 다음, 메모 앞에서 본다`() {
        val formats = AdminContractChannelFormats(existingIds = setOf(1, 2, 3), enabledIds = setOf(1, 2, 3))

        assertMessage("channelFormatIds: 있는 채널 게재 유형 id 를 겹치지 않게 보내 주세요.", create(listOf(1, 9), memo = "가".repeat(501)), formats)
        assertMessage("channelFormatIds: 있는 채널 게재 유형 id 를 겹치지 않게 보내 주세요.", create(listOf(1, 1)), formats)
        assertMessage("channelFormatIds: 있는 채널 게재 유형 id 를 겹치지 않게 보내 주세요.", create(listOf(null)), formats)
        assertEquals(listOf(3L, 1L), create(listOf(3, 1)).toDraft().toTerms(formats).channelFormatIds)
        assertEquals(emptyList<Long>(), create(null).toDraft().toTerms(formats).channelFormatIds)
    }

    @Test
    fun `사용 안 함인 유형은 새로 고를 수 없고 이미 산 것은 남길 수 있다`() {
        val formats = AdminContractChannelFormats(existingIds = setOf(1, 2), enabledIds = setOf(1), previousIds = setOf(2))

        assertEquals(listOf(1L, 2L), create(listOf(1, 2)).toDraft().toTerms(formats).channelFormatIds)
        assertMessage(
            "channelFormatIds: 사용 안 함인 게재 유형은 새로 고를 수 없습니다.",
            create(listOf(2)),
            formats.copy(previousIds = emptySet()),
        )
    }

    private fun assertMessage(message: String, command: AdminAdvertiserContractCreateCommand, formats: AdminContractChannelFormats) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) { command.toDraft().toTerms(formats) }
        assertEquals(message, exception.message)
    }

    private fun create(channelFormatIds: List<Long?>?, memo: String? = null) = AdminAdvertiserContractCreateCommand(
        name = null,
        startAt = START,
        endAt = START.plusMonths(1),
        amount = 1_000_000,
        vatIncluded = true,
        paid = false,
        paidDate = null,
        products = listOf(AdContractProduct.BANNER_HOME_TOP),
        memo = memo,
        channelFormatIds = channelFormatIds,
    )

    private companion object {
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
