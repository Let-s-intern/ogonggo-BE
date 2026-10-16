package com.ogonggo.userapi.auth.implement

import com.ogonggo.core.user.domain.UserInflowSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LetsCareerUserInflowSourceTest {

    @Test
    fun `렛츠커리어에서 처음 로그인했으면 렛츠커리어 기존 회원이다`() {
        assertEquals(UserInflowSource.LETSCAREER, user("LETSCAREER").knownInflowSource(OGONGGO))
        assertEquals(UserInflowSource.LETSCAREER, user("LETSCAREER").knownInflowSource(null))
    }

    @Test
    fun `오공고에서 처음 로그인했으면 오공고 신규 유입이다`() {
        assertEquals(UserInflowSource.OGONGGO, user(OGONGGO).knownInflowSource(OGONGGO))
    }

    @Test
    fun `다른 렛츠커리어 서비스에서 처음 로그인했으면 렛츠커리어 기존 회원이다`() {
        assertEquals(UserInflowSource.LETSCAREER, user("FreeSeminarVodHub").knownInflowSource(OGONGGO))
    }

    @Test
    fun `첫 로그인 서비스가 없거나 오공고 서비스 이름을 모르면 정하지 않는다`() {
        assertNull(user(null).knownInflowSource(OGONGGO))
        assertNull(user(OGONGGO).knownInflowSource(null))
        assertNull(user("FreeSeminarVodHub").knownInflowSource(" "))
    }

    private fun user(firstSignInService: String?): LetsCareerUser = LetsCareerUser(
        userId = 1L,
        email = null,
        name = null,
        phoneNum = null,
        authProvider = null,
        nickname = null,
        profileImageUrl = null,
        isAdmin = false,
        updatedAt = null,
        firstSignInService = firstSignInService,
    )

    companion object {
        private const val OGONGGO = "오공고"
    }
}
