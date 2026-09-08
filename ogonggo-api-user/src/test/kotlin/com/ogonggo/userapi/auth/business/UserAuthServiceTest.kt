package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.LetsCareerJobProfile
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class UserAuthServiceTest {

    private val letsCareerAuthClient = Mockito.mock(LetsCareerAuthClient::class.java)
    private val userReader = Mockito.mock(UserReader::class.java)
    private val letsCareerUserClient = Mockito.mock(LetsCareerUserClient::class.java)
    private val userAppender = Mockito.mock(UserAppender::class.java)
    private val userProfileManager = Mockito.mock(UserProfileManager::class.java)
    private val tokenProvider = Mockito.mock(OgonggoTokenProvider::class.java)
    private val refreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T01:00:00Z"), ZONE)
    private val transactionTemplate = TransactionTemplate(NoOpTransactionManager())

    private val service = UserAuthService(
        letsCareerAuthClient = letsCareerAuthClient,
        userReader = userReader,
        letsCareerUserClient = letsCareerUserClient,
        userAppender = userAppender,
        userProfileManager = userProfileManager,
        tokenProvider = tokenProvider,
        refreshTokenStore = refreshTokenStore,
        signInValidator = SignInValidator(),
        jwtProperties = JWT_PROPERTIES,
        transactionTemplate = transactionTemplate,
        clock = clock,
    )

    @Test
    fun `첫 로그인이면 계정을 만들고 신규 사용자로 응답한다`() {
        stubLetsCareerUser()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(null)
        Mockito.`when`(userAppender.append(UserAppendDto(LETSCAREER_USER_ID, NOW)))
            .thenReturn(activeAccount())
        stubIssuedTokens()

        val result = service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        assertTrue(result.isNewUser)
        assertEquals("og-access", result.tokens.accessToken)
        assertEquals("og-refresh", result.tokens.refreshToken)
        Mockito.verify(userAppender).append(UserAppendDto(LETSCAREER_USER_ID, NOW))
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
            UserProfileSyncDto(
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

    private fun activeAccount(status: UserStatus = UserStatus.ACTIVE): UserAccountDto =
        UserAccountDto(
            userId = USER_ID,
            letsCareerUserId = LETSCAREER_USER_ID,
            email = null,
            status = status,
            role = UserRole.USER,
            joinedAt = JOINED_AT,
        )

    @Test
    fun `첫 로그인에만 렛츠커리어의 학력과 희망 조건을 복제한다`() {
        stubLetsCareerUser()
        stubIssuedTokens()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(null)
        Mockito.`when`(userAppender.append(UserAppendDto(LETSCAREER_USER_ID, NOW)))
            .thenReturn(activeAccount())
        Mockito.`when`(letsCareerUserClient.readJobProfile(LETSCAREER_USER_ID)).thenReturn(
            LetsCareerJobProfile(
                university = "오공고대학교",
                major = "컴퓨터공학과",
                grade = UserGrade.GRADUATE,
                wishField = "개발",
                wishJob = null,
                wishIndustry = null,
                wishEmploymentType = null,
                wishCompany = null,
            ),
        )

        service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        Mockito.verify(userProfileManager).replaceJobInfo(
            USER_ID,
            UserProfileJobInfoDto(
                university = "오공고대학교",
                major = "컴퓨터공학과",
                grade = UserGrade.GRADUATE,
                wishField = "개발",
                wishJob = null,
                wishIndustry = null,
                wishEmploymentType = null,
                wishCompany = null,
            ),
            NOW,
        )
    }

    @Test
    fun `재로그인에서는 학력과 희망 조건을 가져오지 않는다`() {
        stubLetsCareerUser()
        stubIssuedTokens()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(activeAccount())

        service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        Mockito.verifyNoInteractions(letsCareerUserClient)
        Mockito.verify(userProfileManager, Mockito.never())
            .replaceJobInfo(USER_ID, EMPTY_JOB_INFO, NOW)
    }

    @Test
    fun `렛츠커리어 조회가 실패해도 가입은 성공한다`() {
        stubLetsCareerUser()
        stubIssuedTokens()
        Mockito.`when`(userReader.readByLetsCareerUserId(LETSCAREER_USER_ID)).thenReturn(null)
        Mockito.`when`(userAppender.append(UserAppendDto(LETSCAREER_USER_ID, NOW)))
            .thenReturn(activeAccount())
        Mockito.`when`(letsCareerUserClient.readJobProfile(LETSCAREER_USER_ID)).thenReturn(null)

        val result = service.signInWithLetsCareer(LC_ACCESS_TOKEN)

        assertEquals(true, result.isNewUser)
        Mockito.verify(userProfileManager, Mockito.never())
            .replaceJobInfo(USER_ID, EMPTY_JOB_INFO, NOW)
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 27, 10, 0)
        private val JOINED_AT: LocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0)
        private val EMPTY_JOB_INFO = UserProfileJobInfoDto(null, null, null, null, null, null, null, null)
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

/** 트랜잭션 경계만 흉내낸다. 이 테스트는 경계 안에서 무엇을 호출하는지만 검증한다. */
private class NoOpTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
    override fun commit(status: TransactionStatus) = Unit
    override fun rollback(status: TransactionStatus) = Unit
}