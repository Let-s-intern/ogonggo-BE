package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserAccount
import com.ogonggo.core.user.implement.UserAppendCommand
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileSyncCommand
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.LetsCareerAuthClient
import com.ogonggo.userapi.auth.implement.LetsCareerUser
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.auth.implement.SignInValidator
import com.ogonggo.userapi.auth.error.AuthErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class UserAuthServiceTest {

    private val letsCareerAuthClient = Mockito.mock(LetsCareerAuthClient::class.java)
    private val userReader = Mockito.mock(UserReader::class.java)
    private val userAppender = Mockito.mock(UserAppender::class.java)
    private val userProfileManager = Mockito.mock(UserProfileManager::class.java)
    private val tokenProvider = Mockito.mock(OgonggoTokenProvider::class.java)
    private val refreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T01:00:00Z"), ZONE)

    private val service = UserAuthService(
        letsCareerAuthClient = letsCareerAuthClient,
        userReader = userReader,
        userAppender = userAppender,
        userProfileManager = userProfileManager,
        tokenProvider = tokenProvider,
        refreshTokenStore = refreshTokenStore,
        signInValidator = SignInValidator(),
        jwtProperties = JWT_PROPERTIES,
        clock = clock,
    )

    @Test
    fun `첫 로그인이면 계정을 만들고 신규 사용자로 응답한다`() {
        stubLetsCareerUser()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(null)
        Mockito.`when`(userAppender.append(UserAppendCommand(LETSCAREER_USER_ID, NOW)))
            .thenReturn(activeAccount())
        stubIssuedTokens()

        val result = service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        assertTrue(result.isNewUser)
        assertEquals("og-access", result.tokens.accessToken)
        assertEquals("og-refresh", result.tokens.refreshToken)
        Mockito.verify(userAppender).append(UserAppendCommand(LETSCAREER_USER_ID, NOW))
        Mockito.verify(refreshTokenStore).save(USER_ID, "og-refresh", JWT_PROPERTIES.refreshTokenValidity)
    }

    @Test
    fun `이미 가입한 사용자는 계정을 만들지 않고 프로필만 동기화한다`() {
        stubLetsCareerUser()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(activeAccount())
        stubIssuedTokens()

        val result = service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        assertFalse(result.isNewUser)
        Mockito.verifyNoInteractions(userAppender)
        Mockito.verify(userProfileManager).sync(
            UserProfileSyncCommand(
                userId = USER_ID,
                name = "김렛츠",
                email = "lets@career.co.kr",
                nickname = "렛츠",
                profileImageUrl = null,
                letsCareerUpdatedAt = LETSCAREER_UPDATED_AT,
                syncedAt = NOW,
            ),
        )
    }

    @Test
    fun `정지된 사용자는 토큰을 발급받지 못한다`() {
        stubLetsCareerUser()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID))
            .thenReturn(activeAccount(status = UserStatus.SUSPENDED))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.signInWithLetsCareer(LC_ACCESS_TOKEN)
        }

        assertEquals(UserErrorCode.USER_SUSPENDED, exception.errorCode)
        Mockito.verifyNoInteractions(refreshTokenStore)
    }

    @Test
    fun `탈퇴한 사용자는 토큰을 발급받지 못한다`() {
        stubLetsCareerUser()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID))
            .thenReturn(activeAccount(status = UserStatus.WITHDRAWN))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.signInWithLetsCareer(LC_ACCESS_TOKEN)
        }

        assertEquals(UserErrorCode.USER_WITHDRAWN, exception.errorCode)
    }

    @Test
    fun `재발급은 렛츠커리어를 호출하지 않고 액세스 토큰만 새로 만든다`() {
        Mockito.`when`(tokenProvider.parseRefreshToken("og-refresh")).thenReturn(USER_ID)
        Mockito.`when`(refreshTokenStore.matches(USER_ID, "og-refresh")).thenReturn(true)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeAccount())
        Mockito.`when`(tokenProvider.createAccessToken(USER_ID)).thenReturn("og-access-2")

        assertEquals("og-access-2", service.reissueAccessToken("og-refresh"))

        Mockito.verifyNoInteractions(letsCareerAuthClient)
    }

    @Test
    fun `로그아웃된 리프레시 토큰으로는 재발급하지 않는다`() {
        Mockito.`when`(tokenProvider.parseRefreshToken("og-refresh")).thenReturn(USER_ID)
        Mockito.`when`(refreshTokenStore.matches(USER_ID, "og-refresh")).thenReturn(false)

        val exception = assertThrows(UnauthorizedException::class.java) {
            service.reissueAccessToken("og-refresh")
        }

        assertEquals(AuthErrorCode.EXPIRED_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `로그아웃은 리프레시 토큰만 지운다`() {
        service.signOut(USER_ID)

        Mockito.verify(refreshTokenStore).delete(USER_ID)
        Mockito.verifyNoInteractions(tokenProvider)
    }

    private fun stubLetsCareerUser() {
        Mockito.`when`(letsCareerAuthClient.verify(LC_ACCESS_TOKEN)).thenReturn(
            LetsCareerUser(
                userId = LETSCAREER_USER_ID,
                email = "lets@career.co.kr",
                name = "김렛츠",
                nickname = "렛츠",
                profileImageUrl = null,
                isAdmin = false,
                updatedAt = LETSCAREER_UPDATED_AT,
            ),
        )
    }

    private fun stubIssuedTokens() {
        Mockito.`when`(tokenProvider.createAccessToken(USER_ID)).thenReturn("og-access")
        Mockito.`when`(tokenProvider.createRefreshToken(USER_ID)).thenReturn("og-refresh")
    }

    private fun activeAccount(status: UserStatus = UserStatus.ACTIVE): UserAccount =
        UserAccount(
            userId = USER_ID,
            letsCareerUserId = LETSCAREER_USER_ID,
            email = null,
            status = status,
            role = UserRole.USER,
        )

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 27, 10, 0)
        private val LETSCAREER_UPDATED_AT: LocalDateTime = LocalDateTime.of(2026, 8, 20, 9, 0)
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private const val LC_ACCESS_TOKEN = "lc-access-token"
        private val JWT_PROPERTIES = JwtProperties(
            secret = "b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
            accessTokenValidity = Duration.ofMinutes(30),
            refreshTokenValidity = Duration.ofDays(14),
        )
    }
}
