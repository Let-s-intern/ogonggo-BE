package com.ogonggo.adminapi.adchannelformat.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdminAdChannelFormatCommandsTest {

    @Test
    fun `등록 본문은 화면 문구로 처음 걸린 칸 하나만 알린다`() {
        assertMessage("platform: INSTAGRAM, KAKAO_OPENCHAT 가운데 하나로 보내 주세요.", create(platform = null))
        assertMessage("name: 1~50자로 보내 주세요.", create(name = "  "))
        assertMessage("name: 같은 플랫폼에 같은 이름의 게재 유형이 있습니다.", create(name = " 스토리 ", defaultPrice = -1))
        assertMessage("description: 500자까지 보낼 수 있습니다.", create(description = "가".repeat(501)))
        assertMessage("defaultPrice: null 이거나 0 이상의 정수(원)로 보내 주세요.", create(defaultPrice = -1))
        assertMessage("defaultPrice: null 이거나 0 이상의 정수(원)로 보내 주세요.", create(defaultPrice = 10_000_000_000))
        assertMessage("channelIds: 같은 플랫폼에 있는 채널 id 만 보내 주세요.", create(channelIds = listOf(1, 6)))
        assertMessage("channelIds: 같은 플랫폼에 있는 채널 id 만 보내 주세요.", create(channelIds = listOf(99)))
    }

    @Test
    fun `채널을 보내지 않으면 모든 채널이고 사용 여부를 보내지 않으면 켠다`() {
        val (_, terms) = create(channelIds = null).toDraft().toTerms({ _, _ -> false }, CHANNELS)

        assertEquals(emptyList<Long>(), terms.channelIds)
        assertEquals(true, terms.enabled)
    }

    @Test
    fun `순서 바꾸기는 그 플랫폼의 유형을 빠짐없이 겹치지 않게 받는다`() {
        val current = listOf(6L, 7L, 8L)

        assertEquals(listOf(8L, 6L, 7L), AdminAdChannelFormatOrderCommand(AdChannelPlatform.KAKAO_OPENCHAT, listOf(8, 6, 7)).validFormatIds(current))
        listOf(listOf(8L, 6L), listOf(8L, 6L, 6L), listOf(8L, 6L, 1L), null).forEach { formatIds ->
            val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
                AdminAdChannelFormatOrderCommand(AdChannelPlatform.KAKAO_OPENCHAT, formatIds).validFormatIds(current)
            }
            assertEquals("formatIds: 그 플랫폼의 게재 유형 id 를 빠짐없이, 겹치지 않게 보내 주세요.", exception.message)
        }
    }

    private fun assertMessage(message: String, command: AdminAdChannelFormatCreateCommand) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            command.toDraft().toTerms({ platform, name -> platform == AdChannelPlatform.INSTAGRAM && name == "스토리" }, CHANNELS)
        }
        assertEquals(message, exception.message)
    }

    private fun create(
        platform: AdChannelPlatform? = AdChannelPlatform.INSTAGRAM,
        name: String? = "캐러셀",
        description: String? = null,
        defaultPrice: Long? = 350_000,
        channelIds: List<Long?>? = listOf(1, 2),
    ) = AdminAdChannelFormatCreateCommand(
        platform = platform,
        name = name,
        description = description,
        enabled = null,
        defaultPrice = defaultPrice,
        channelIds = channelIds,
    )

    private companion object {
        val CHANNELS = mapOf(
            1L to AdChannelPlatform.INSTAGRAM,
            2L to AdChannelPlatform.INSTAGRAM,
            6L to AdChannelPlatform.KAKAO_OPENCHAT,
        )
    }
}
