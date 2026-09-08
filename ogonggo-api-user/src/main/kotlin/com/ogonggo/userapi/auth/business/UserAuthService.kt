package com.ogonggo.userapi.auth.business

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import com.ogonggo.userapi.auth.implement.LetsCareerAuthClient
import com.ogonggo.userapi.auth.implement.LetsCareerUser
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.auth.implement.SignInValidator
import com.ogonggo.userapi.auth.error.AuthErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserAuthService(
    private val letsCareerAuthClient: LetsCareerAuthClient,
    private val userReader: UserReader,
    private val letsCareerUserClient: LetsCareerUserClient,
    private val userAppender: UserAppender,
    private val userProfileManager: UserProfileManager,
    private val tokenProvider: OgonggoTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val signInValidator: SignInValidator,
    private val jwtProperties: JwtProperties,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    /**
     * 렛츠커리어 액세스 토큰을 오공고 세션으로 교환한다.
     * 오공고에 계정이 없으면 이 시점에 만들고, 있으면 프로필만 동기화한다.
     *
     * 계정과 프로필을 다루는 구간만 트랜잭션으로 묶는다.
     * 렛츠커리어 호출을 트랜잭션 안에 두면 응답을 기다리는 동안 DB 커넥션을 잡고 있어,
     * 렛츠커리어가 느려질 때 로그인과 무관한 API까지 커넥션이 없어 함께 실패한다.
     * 토큰 발급도 DB를 쓰지 않으므로 커밋 이후로 보낸다.
     */
    fun signInWithLetsCareer(letsCareerAccessToken: String): SignInResult {
        val letsCareerUser = letsCareerAuthClient.verify(letsCareerAccessToken)
        val now = LocalDateTime.now(clock)

        val account = checkNotNull(transactionTemplate.execute { synchronizeAccount(letsCareerUser, now) })

        if (account.isNewUser) {
            seedJobProfile(account.userId, letsCareerUser.userId, now)
        }

        return SignInResult(
            tokens = issueTokens(account.userId),
            isNewUser = account.isNewUser,
        )
    }

    private fun synchronizeAccount(letsCareerUser: LetsCareerUser, now: LocalDateTime): SynchronizedAccount {
        val existingAccount = userReader.readByLetsCareerUserId(letsCareerUser.userId)
        val account = existingAccount ?: userAppender.append(
            UserAppendDto(letsCareerUserId = letsCareerUser.userId, joinedAt = now),
        )
        signInValidator.validate(account.status)

        userProfileManager.sync(letsCareerUser.toSyncCommand(account.userId, now))

        return SynchronizedAccount(userId = account.userId, isNewUser = existingAccount == null)
    }

    /**
     * 최초 가입 때 한 번만 렛츠커리어의 학력과 희망 조건을 복제한다.
     * 그 뒤로는 오공고가 소유하므로 재로그인에서는 가져오지 않는다. 사용자가 오공고에서 고친 값을 지우지 않기 위한 것이다.
     *
     * 렛츠커리어 호출은 트랜잭션 밖에서 한다. 응답을 기다리는 동안 DB 커넥션을 잡고 있으면
     * 렛츠커리어가 느려질 때 로그인과 무관한 API까지 커넥션이 없어 함께 실패한다.
     *
     * 실패해도 가입을 되돌리지 않는다. 학력과 희망 조건은 로그인의 성공 조건이 아니고,
     * 비어 있으면 사용자가 오공고에서 직접 입력하면 된다.
     */
    private fun seedJobProfile(userId: Long, letsCareerUserId: Long, now: LocalDateTime) {
        val jobProfile = letsCareerUserClient.readJobProfile(letsCareerUserId) ?: return

        try {
            transactionTemplate.execute { userProfileManager.replaceJobInfo(userId, jobProfile.toCommand(), now) }
        } catch (exception: Exception) {
            log.warn("학력·희망 조건 복제에 실패했습니다. userId={}", userId, exception)
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(UserAuthService::class.java)
    }
}

private fun LetsCareerUser.toSyncCommand(userId: Long, now: LocalDateTime): UserProfileSyncDto =
    UserProfileSyncDto(
        userId = userId,
        name = name,
        email = email,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
        letsCareerUpdatedAt = updatedAt,
        syncedAt = now,
    )

private data class SynchronizedAccount(val userId: Long, val isNewUser: Boolean)