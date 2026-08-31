package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.implement.UserAppendCommand
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileSyncCommand
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.implement.LetsCareerAuthClient
import com.ogonggo.userapi.auth.implement.LetsCareerUser
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.auth.implement.SignInValidator
import com.ogonggo.userapi.auth.error.AuthErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserAuthService(
    private val letsCareerAuthClient: LetsCareerAuthClient,
    private val userReader: UserReader,
    private val userAppender: UserAppender,
    private val userProfileManager: UserProfileManager,
    private val tokenProvider: OgonggoTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val signInValidator: SignInValidator,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {

    /**
     * 렛츠커리어 액세스 토큰을 오공고 세션으로 교환한다.
     * 오공고에 계정이 없으면 이 시점에 만들고, 있으면 프로필만 동기화한다.
     */
    @Transactional
    fun signInWithLetsCareer(letsCareerAccessToken: String): SignInResult {
        val letsCareerUser = letsCareerAuthClient.verify(letsCareerAccessToken)
        val now = LocalDateTime.now(clock)

        val existingAccount = userReader.readByLetsCareerUserId(letsCareerUser.userId)
        val account = existingAccount ?: userAppender.append(
            UserAppendCommand(letsCareerUserId = letsCareerUser.userId, joinedAt = now),
        )
        signInValidator.validate(account.status)

        userProfileManager.sync(letsCareerUser.toSyncCommand(account.userId, now))

        return SignInResult(
            tokens = issueTokens(account.userId),
            isNewUser = existingAccount == null,
        )
    }

    /**
     * 오공고 리프레시 토큰으로 액세스 토큰을 재발급한다.
     * 렛츠커리어를 다시 호출하지 않으므로 렛츠커리어 장애 중에도 세션이 유지된다.
     */
    fun reissueAccessToken(refreshToken: String): String {
        val userId = tokenProvider.parseRefreshToken(refreshToken)

        if (!refreshTokenStore.matches(userId, refreshToken)) {
            throw UnauthorizedException(AuthErrorCode.EXPIRED_REFRESH_TOKEN)
        }

        signInValidator.validate(userReader.read(userId).status)

        return tokenProvider.createAccessToken(userId)
    }

    /**
     * 리프레시 토큰만 폐기한다. 이미 발급된 액세스 토큰은 만료될 때까지 유효하다.
     */
    fun signOut(userId: Long) {
        refreshTokenStore.delete(userId)
    }

    private fun issueTokens(userId: Long): AuthTokens {
        val accessToken = tokenProvider.createAccessToken(userId)
        val refreshToken = tokenProvider.createRefreshToken(userId)
        refreshTokenStore.save(userId, refreshToken, jwtProperties.refreshTokenValidity)
        return AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
    }
}

private fun LetsCareerUser.toSyncCommand(userId: Long, now: LocalDateTime): UserProfileSyncCommand =
    UserProfileSyncCommand(
        userId = userId,
        name = name,
        email = email,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
        letsCareerUpdatedAt = updatedAt,
        syncedAt = now,
    )
