package com.ogonggo.adminapi.adchannel.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional

class AdminAdChannelCommandsTest {

    @Test
    fun `등록 본문은 화면 문구로 처음 걸린 칸 하나만 알린다`() {
        assertMessage("platform: INSTAGRAM, KAKAO_OPENCHAT 가운데 하나로 보내 주세요.", create(platform = null, name = null))
        assertMessage("handle: 영문·숫자·마침표·밑줄 30자까지로 보내 주세요(`@` 는 빼도 됩니다).", create(handle = null, name = null))
        assertMessage("handle: 영문·숫자·마침표·밑줄 30자까지로 보내 주세요(`@` 는 빼도 됩니다).", create(handle = "lets career"))
        assertMessage("handle: 이미 등록된 계정입니다.", create(handle = "@LetsCareer.Job", name = null), taken = setOf("LetsCareer.Job"))
        assertMessage("name: 1~50자로 보내 주세요.", create(name = " "))
        assertMessage("name: 1~50자로 보내 주세요.", create(name = "가".repeat(51)))
        assertMessage("url: http:// 또는 https:// 로 시작하는 주소를 보내 주세요.", create(url = "instagram.com/letscareer.job"))
        assertMessage("profileImageUrl: null 이거나 https:// 주소, / 로 시작하는 경로로 보내 주세요.", create(profileImageUrl = "community/a.jpg"))
        assertMessage("memberCount: null 이거나 0 이상의 정수로 보내 주세요.", create(memberCount = -1))
        assertMessage("memo: 500자까지 보낼 수 있습니다.", create(memo = "가".repeat(501)))
    }

    @Test
    fun `계정 이름은 앞의 @를 떼고 저장하며 오픈채팅은 보내도 버리고 사용 여부를 보내지 않으면 켠다`() {
        val (_, instagram) = create(handle = " @letscareer.job ").toDraft().toProfile { false }
        val (platform, openChat) = create(platform = AdChannelPlatform.KAKAO_OPENCHAT, handle = "아무 값").toDraft().toProfile { false }

        assertEquals("letscareer.job", instagram.handle)
        assertEquals(true, instagram.enabled)
        assertEquals(AdChannelPlatform.KAKAO_OPENCHAT, platform)
        assertNull(openChat.handle)
    }

    @Test
    fun `수정은 플랫폼을 바꿀 수 없고 null을 보낸 지울 수 있는 칸은 지운다`() {
        // given
        val stored = Mockito.mock(AdChannel::class.java)
        Mockito.`when`(stored.platform).thenReturn(AdChannelPlatform.INSTAGRAM)
        Mockito.`when`(stored.name).thenReturn("렛츠커리어 오공고")
        Mockito.`when`(stored.handle).thenReturn("letscareer.job")
        Mockito.`when`(stored.url).thenReturn("https://www.instagram.com/letscareer.job")
        Mockito.`when`(stored.profileImageUrl).thenReturn("/community/a.jpg")
        Mockito.`when`(stored.memberCount).thenReturn(100)
        Mockito.`when`(stored.enabled).thenReturn(true)
        Mockito.`when`(stored.memo).thenReturn("메모")

        // when
        val (_, profile) = AdminAdChannelUpdateCommand(profileImageUrl = Optional.empty(), memberCount = Optional.empty(), enabled = false)
            .mergeInto(stored)
            .toProfile { false }

        // then
        assertNull(profile.profileImageUrl)
        assertNull(profile.memberCount)
        assertEquals(false, profile.enabled)
        assertEquals("메모", profile.memo)
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            AdminAdChannelUpdateCommand(platform = AdChannelPlatform.KAKAO_OPENCHAT).mergeInto(stored)
        }
        assertEquals("platform: 등록한 뒤에는 바꿀 수 없습니다.", exception.message)
    }

    private fun assertMessage(message: String, command: AdminAdChannelCreateCommand, taken: Set<String> = emptySet()) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
            command.toDraft().toProfile { it in taken }
        }
        assertEquals(message, exception.message)
    }

    private fun create(
        platform: AdChannelPlatform? = AdChannelPlatform.INSTAGRAM,
        name: String? = "렛츠커리어 오공고",
        handle: String? = "letscareer.job",
        url: String? = "https://www.instagram.com/letscareer.job",
        profileImageUrl: String? = null,
        memberCount: Long? = null,
        memo: String? = null,
    ) = AdminAdChannelCreateCommand(
        platform = platform,
        name = name,
        handle = handle,
        url = url,
        profileImageUrl = profileImageUrl,
        memberCount = memberCount,
        enabled = null,
        memo = memo,
    )
}
