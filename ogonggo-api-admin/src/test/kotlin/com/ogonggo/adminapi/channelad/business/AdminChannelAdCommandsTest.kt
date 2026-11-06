package com.ogonggo.adminapi.channelad.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional

class AdminChannelAdCommandsTest {

    @Test
    fun `달은 그달 1일부터 다음 달 1일 전까지로 바꾸고 형식이 틀리면 화면 문구로 알린다`() {
        val condition = AdminChannelAdSearchCommand(month = "2026-12").toCondition()

        assertEquals(LocalDate.of(2026, 12, 1), condition.from)
        assertEquals(LocalDate.of(2027, 1, 1), condition.toExclusive)
        assertNull(AdminChannelAdSearchCommand(month = "").toCondition().from)
        listOf("2026-13", "2026-1", "202611").forEach { month ->
            val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
                AdminChannelAdSearchCommand(month = month).toCondition()
            }
            assertEquals("month: YYYY-MM 로 보내 주세요.", exception.message)
        }
    }

    @Test
    fun `날짜·상태·주소·메모는 화면 문구로 처음 걸린 칸 하나만 알린다`() {
        assertMessage("postDate: YYYY-MM-DD(KST) 로 보내 주세요.", draft(postDate = null, endDate = POST_DATE.minusDays(1)))
        assertMessage("endDate: null 이거나 게시 예정일과 같거나 뒤인 YYYY-MM-DD 로 보내 주세요.", draft(endDate = POST_DATE.minusDays(1)))
        assertMessage("postUrl: null 이거나 http:// 또는 https:// 로 시작하는 주소로 보내 주세요.", draft(postUrl = "instagram.com/p/1"))
        assertMessage("creativeMemo: 1000자까지 보낼 수 있습니다.", draft(creativeMemo = "가".repeat(1001)))
        assertMessage("adminMemo: 1000자까지 보낼 수 있습니다.", draft(adminMemo = "가".repeat(1001)))
        assertEquals(ChannelAdStatus.SCHEDULED, draft().toTerms(1, AdChannelPlatform.INSTAGRAM, 1, 1).status)
        assertEquals(POST_DATE, draft(endDate = POST_DATE).toTerms(1, AdChannelPlatform.INSTAGRAM, 1, 1).endDate)
    }

    @Test
    fun `수정에서 광고주만 바꾸고 계약을 보내지 않으면 계약을 끊고 null을 보낸 칸은 지운다`() {
        // given
        val stored = Mockito.mock(ChannelAd::class.java)
        Mockito.`when`(stored.advertiserId).thenReturn(1)
        Mockito.`when`(stored.contractId).thenReturn(10)
        Mockito.`when`(stored.channelId).thenReturn(2)
        Mockito.`when`(stored.formatId).thenReturn(3)
        Mockito.`when`(stored.postDate).thenReturn(POST_DATE)
        Mockito.`when`(stored.endDate).thenReturn(POST_DATE.plusDays(3))
        Mockito.`when`(stored.status).thenReturn(ChannelAdStatus.SCHEDULED)
        Mockito.`when`(stored.postUrl).thenReturn("https://www.instagram.com/p/1")

        // when
        val advertiserChanged = AdminChannelAdUpdateCommand(advertiserId = 2).mergeInto(stored)
        val sameAdvertiser = AdminChannelAdUpdateCommand(advertiserId = 1, endDate = Optional.empty()).mergeInto(stored)
        val withContract = AdminChannelAdUpdateCommand(advertiserId = 2, contractId = Optional.of(20)).mergeInto(stored)

        // then
        assertNull(advertiserChanged.contractId)
        assertEquals(10L, sameAdvertiser.contractId)
        assertNull(sameAdvertiser.endDate)
        assertEquals("https://www.instagram.com/p/1", sameAdvertiser.postUrl)
        assertEquals(20L, withContract.contractId)
    }

    private fun assertMessage(message: String, draft: AdminChannelAdDraft) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            draft.toTerms(1, AdChannelPlatform.INSTAGRAM, 1, 1)
        }
        assertEquals(message, exception.message)
    }

    private fun draft(
        postDate: LocalDate? = POST_DATE,
        endDate: LocalDate? = null,
        postUrl: String? = null,
        creativeMemo: String? = null,
        adminMemo: String? = null,
    ) = AdminChannelAdCreateCommand(
        advertiserId = 1,
        contractId = null,
        channelId = 1,
        formatId = 1,
        postDate = postDate,
        endDate = endDate,
        status = null,
        postUrl = postUrl,
        creativeMemo = creativeMemo,
        adminMemo = adminMemo,
    ).toDraft()

    private companion object {
        val POST_DATE: LocalDate = LocalDate.of(2026, 11, 10)
    }
}
