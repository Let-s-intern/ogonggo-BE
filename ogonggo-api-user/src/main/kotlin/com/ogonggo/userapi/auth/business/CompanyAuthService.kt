package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.implement.CompanyAccountAppendCommand
import com.ogonggo.core.user.implement.CompanyProfileAppendCommand
import com.ogonggo.core.user.implement.CompanyProfileAppender
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.auth.implement.SignInValidator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 기업 회원은 렛츠커리어를 거치지 않고 오공고가 직접 인증한다.
 * 발급하는 토큰은 렛츠커리어 교환 로그인과 같은 것이라 재발급·로그아웃 경로를 그대로 공유한다.
 */
@Service
class CompanyAuthService(
    private val userReader: UserReader,
    private val userAppender: UserAppender,
    private val companyProfileAppender: CompanyProfileAppender,
    private val tokenProvider: OgonggoTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val signInValidator: SignInValidator,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {

    /**
     * 승인 절차와 이메일 인증이 없으므로 계정과 기업 정보를 한 트랜잭션에서 만들고 바로 세션을 발급한다.
     */
    @Transactional
    fun signUp(command: CompanySignUpCommand): AuthTokens {
        val now = LocalDateTime.now(clock)

        val account = userAppender.appendCompany(
            CompanyAccountAppendCommand(
                email = command.email,
                encodedPassword = passwordEncoder.encode(command.password),
                joinedAt = now,
            ),
        )

        companyProfileAppender.append(
            CompanyProfileAppendCommand(
                userId = account.userId,
                organizationName = command.organizationName,
                managerName = command.managerName,
            ),
        )

        return issueTokens(account.userId)
    }

    /**
     * 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해 응답하지 않는다.
     * 구분하면 이 엔드포인트가 계정 존재 확인 수단이 된다.
     */
    fun signIn(command: CompanySignInCommand): AuthTokens {
        val credential = userReader.readCredentialByEmail(command.email)
            ?: throw UnauthorizedException(AuthErrorCode.INVALID_COMPANY_CREDENTIALS)

        if (!passwordEncoder.matches(command.password, credential.encodedPassword)) {
            throw UnauthorizedException(AuthErrorCode.INVALID_COMPANY_CREDENTIALS)
        }

        signInValidator.validate(credential.status)

        return issueTokens(credential.userId)
    }

    private fun issueTokens(userId: Long): AuthTokens {
        val accessToken = tokenProvider.createAccessToken(userId)
        val refreshToken = tokenProvider.createRefreshToken(userId)
        refreshTokenStore.save(userId, refreshToken, jwtProperties.refreshTokenValidity)
        return AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
    }
}

data class CompanySignUpCommand(
    val email: String,
    val password: String,
    val organizationName: String,
    val managerName: String,
)

data class CompanySignInCommand(
    val email: String,
    val password: String,
)
