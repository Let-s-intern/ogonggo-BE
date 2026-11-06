package com.ogonggo.core.adchannelformat.domain

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdChannelFormatDomainTest {

    @Test
    fun `올릴 수 있는 채널이 비어 있으면 그 플랫폼의 모든 채널에 올릴 수 있다`() {
        assertTrue(format(channelIds = emptyList()).allows(7))
        assertTrue(format(channelIds = listOf(1, 2, 5)).allows(5))
        assertFalse(format(channelIds = listOf(1, 2, 5)).allows(3))
    }

    @Test
    fun `지운 채널을 빼서 남은 채널이 없으면 모든 채널에 올릴 수 있다`() {
        // given
        val format = format(channelIds = listOf(5))

        // when
        format.removeChannel(5)

        // then
        assertEquals(emptyList<Long>(), format.channelIds)
        assertTrue(format.allows(3))
    }

    @Test
    fun `칸은 이름을 다듬고 올릴 수 있는 채널을 겹치지 않게 식별자 순으로 두며 단가는 0원부터다`() {
        val terms = AdChannelFormatTerms(" 릴스(숏츠) ", " ", true, 500_000, listOf(5, 1, 5))

        assertEquals("릴스(숏츠)", terms.name)
        assertEquals(null, terms.description)
        assertEquals(listOf(1L, 5L), terms.channelIds)
        assertThrows(IllegalArgumentException::class.java) { AdChannelFormatTerms("피드", null, true, -1, emptyList()) }
    }

    private fun format(channelIds: List<Long>) = AdChannelFormat(
        platform = AdChannelPlatform.INSTAGRAM,
        terms = AdChannelFormatTerms("피드 게시물", null, true, 300_000, channelIds),
        displayOrder = 1,
    )
}
