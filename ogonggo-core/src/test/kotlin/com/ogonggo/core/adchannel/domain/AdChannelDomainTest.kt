package com.ogonggo.core.adchannel.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdChannelDomainTest {

    @Test
    fun `채널 칸은 앞뒤 공백과 계정 이름 앞의 @를 떼고 빈 프로필 이미지·메모는 null로 둔다`() {
        // when
        val profile = profile(name = " 렛츠커리어 오공고 ", handle = " @letscareer.job ", profileImageUrl = " ", memo = "")

        // then
        assertEquals("렛츠커리어 오공고", profile.name)
        assertEquals("letscareer.job", profile.handle)
        assertNull(profile.profileImageUrl)
        assertNull(profile.memo)
    }

    @Test
    fun `인스타그램 채널은 계정 이름이 있어야 하고 오픈채팅방은 계정 이름을 저장하지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) { AdChannel(AdChannelPlatform.INSTAGRAM, profile(handle = null)) }

        val openChat = AdChannel(AdChannelPlatform.KAKAO_OPENCHAT, profile(name = "마케팅 채용공고 큐레이션", handle = "letscareer.job"))

        assertNull(openChat.handle)
        assertEquals("마케팅 채용공고 큐레이션", openChat.label)
    }

    @Test
    fun `프로필 이미지는 http(s) 주소나 웹 사이트 경로만 받는다`() {
        assertEquals("/community/profile.jpg", profile(profileImageUrl = "/community/profile.jpg").profileImageUrl)
        assertThrows(IllegalArgumentException::class.java) { profile(profileImageUrl = "community/profile.jpg") }
        assertThrows(IllegalArgumentException::class.java) { profile(url = "instagram.com/letscareer.job") }
    }

    private fun profile(
        name: String = "렛츠커리어 오공고",
        handle: String? = "letscareer.job",
        url: String = "https://www.instagram.com/letscareer.job",
        profileImageUrl: String? = null,
        memo: String? = null,
    ) = AdChannelProfile(
        name = name,
        handle = handle,
        url = url,
        profileImageUrl = profileImageUrl,
        memberCount = null,
        enabled = true,
        memo = memo,
    )
}
