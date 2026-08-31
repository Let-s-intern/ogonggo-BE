package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.CompanyAccountAppendCommand
import com.ogonggo.core.user.implement.CompanyProfileAppendCommand
import com.ogonggo.core.user.implement.CompanyProfileAppender
import com.ogonggo.core.user.implement.UserAccount
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserCredential
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.auth.implement.SignInValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CompanyAuthServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userAppender = Mockito.mock(UserAppender::class.java)
    private val companyProfileAppender = Mockito.mock(CompanyProfileAppender::class.java)
    private val tokenProvider = Mockito.mock(OgonggoTokenProvider::class.java)
    private val refreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)

    private val service = CompanyAuthService(
        userReader = userReader,
        userAppender = userAppender,
        companyProfileAppender = companyProfileAppender,
        tokenProvider = tokenProvider,
        refreshTokenStore = refreshTokenStore,
        signInValidator = SignInValidator(),
        passwordEncoder = passwordEncoder,
        jwtProperties = JWT_PROPERTIES,
        clock = clock,
    )

    @Test
    fun `가입하면 계정과 기업 정보를 만들고 바로 세션을 발급한다`() {
        Mockito.`when`(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD)
        Mockito.`when`(
            userAppender.appendCompany(
                CompanyAccountAppendCommand(
                    email = EMAIL,
                    encodedPassword = ENCODED_PASSWORD,
                    joinedAt = NOW,
                ),
            ),
        ).thenReturn(companyAccount())
        stubIssuedTokens()

        val tokens = service.signUp(SIGN_UP_COMMAND)

        assertEquals("og-access", tokens.accessToken)
        assertEquals("og-refresh", tokens.refreshToken)
        Mockito.verify(companyProfileAppender).append(
            CompanyProfileAppendCommand(
                userId = USER_ID,
                organizationName = "렛츠커리어",
                managerName = "김담당",
            ),
        )
        Mockito.verify(refreshTokenStore).save(USER_ID, "og-refresh", JWT_PROPERTIES.refreshTokenValidity)
    }

    @Test
    fun `이미 쓰는 이메일이면 기업 정보를 저장하지 않는다`() {
        Mockito.`when`(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD)
        Mockito.`when`(userAppender.appendCompany(Mockito.any(CompanyAccountAppendCommand::class.java) ?: NEVER))
            .thenThrow(ConflictException(UserErrorCode.EMAIL_ALREADY_EXISTS))

        val exception = assertThrows(ConflictException::class.java) { service.signUp(SIGN_UP_COMMAND) }

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.errorCode)
        Mockito.verifyNoInteractions(companyProfileAppender)
    }

    @Test
    fun `이메일과 비밀번호가 맞으면 세션을 발급한다`() {
        Mockito.`when`(userReader.readCredentialByEmail(EMAIL)).thenReturn(credential())
        Mockito.`when`(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true)
        stubIssuedTokens()

        val tokens = service.signIn(CompanySignInCommand(email = EMAIL, password = RAW_PASSWORD))

        assertEquals("og-access", tokens.accessToken)
        assertEquals("og-refresh", tokens.refreshToken)
    }

    @Test
    fun `없는 이메일과 틀린 비밀번호는 같은 오류로 응답한다`() {
        Mockito.`when`(userReader.readCredentialByEmail(EMAIL)).thenReturn(null)

        val unknownEmail = assertThrows(UnauthorizedException::class.java) {
            service.signIn(CompanySignInCommand(email = EMAIL, password = RAW_PASSWORD))
        }

        Mockito.`when`(userReader.readCredentialByEmail(EMAIL)).thenReturn(credential())
        Mockito.`when`(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false)
        val wrongPassword = assertThrows(UnauthorizedException::class.java) {
            service.signIn(CompanySignInCommand(email = EMAIL, password = RAW_PASSWORD))
        }

        assertEquals(AuthErrorCode.INVALID_COMPANY_CREDENTIALS, unknownEmail.errorCode)
        assertEquals(AuthErrorCode.INVALID_COMPANY_CREDENTIALS, wrongPassword.errorCode)
        Mockito.verifyNoInteractions(refreshTokenStore)
    }

    @Test
    fun `정지된 기업 계정은 로그인할 수 없다`() {
        Mockito.`when`(userReader.readCredentialByEmail(EMAIL))
            .thenReturn(credential(status = UserStatus.SUSPENDED))
        Mockito.`when`(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true)

        val exception = assertThrows(ForbiddenException::class.java) {
            service.signIn(CompanySignInCommand(email = EMAIL, password = RAW_PASSWORD))
        }

        assertEquals(UserErrorCode.USER_SUSPENDED, exception.errorCode)
        Mockito.verifyNoInteractions(refreshTokenStore)
    }

    private fun stubIssuedTokens() {
        Mockito.`when`(tokenProvider.createAccessToken(USER_ID)).thenReturn("og-access")
        Mockito.`when`(tokenProvider.createRefreshToken(USER_ID)).thenReturn("og-refresh")
    }

    private fun companyAccount(): UserAccount = UserAccount(
        userId = USER_ID,
        letsCareerUserId = null,
        email = EMAIL,
        status = UserStatus.ACTIVE,
        role = UserRole.COMPANY,
    )

    private fun credential(status: UserStatus = UserStatus.ACTIVE): UserCredential = UserCredential(
        userId = USER_ID,
        encodedPassword = ENCODED_PASSWORD,
        status = status,
        role = UserRole.COMPANY,
    )

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
        private const val USER_ID = 17L
        private const val EMAIL = "company@example.com"
        private const val RAW_PASSWORD = "password1234"
        private const val ENCODED_PASSWORD = "encoded-password"
        private val NEVER = CompanyAccountAppendCommand("", "", NOW)
        private val SIGN_UP_COMMAND = CompanySignUpCommand(
            email = EMAIL,
            password = RAW_PASSWORD,
            organizationName = "렛츠커리어",
            managerName = "김담당",
        )
        private val JWT_PROPERTIES = JwtProperties(
            secret = "b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
            accessTokenValidity = Duration.ofMinutes(30),
            refreshTokenValidity = Duration.ofDays(14),
        )
    }
}
