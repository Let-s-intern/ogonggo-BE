package com.ogonggo.userapi.auth.implement

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.userapi.auth.error.AuthErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class OgonggoTokenProviderTest {

    // JJWT가 만료를 시스템 시각으로 검증하므로 임의의 고정 시각을 쓸 수 없다.
    // 실행 시각을 한 번만 고정해 테스트 안에서는 시간이 흐르지 않도록 한다.
    private val clock = Clock.fixed(Instant.now(), ZoneId.of("Asia/Seoul"))
    private val provider = OgonggoTokenProvider(properties(), clock)

    @Test
    fun `액세스 토큰에서 사용자 식별자를 읽는다`() {
        val token = provider.createAccessToken(17L)

        assertEquals(17L, provider.parseAccessToken(token))
    }

    @Test
    fun `리프레시 토큰을 액세스 토큰으로 사용할 수 없다`() {
        val refreshToken = provider.createRefreshToken(17L)

        val exception = assertThrows(UnauthorizedException::class.java) { provider.parseAccessToken(refreshToken) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `액세스 토큰을 리프레시 토큰으로 사용할 수 없다`() {
        val accessToken = provider.createAccessToken(17L)

        val exception = assertThrows(UnauthorizedException::class.java) { provider.parseRefreshToken(accessToken) }

        assertEquals(AuthErrorCode.NOT_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `다른 시크릿으로 서명된 토큰은 거부한다`() {
        val otherProvider = OgonggoTokenProvider(properties(secret = OTHER_SECRET), clock)
        val foreignToken = otherProvider.createAccessToken(17L)

        val exception = assertThrows(UnauthorizedException::class.java) { provider.parseAccessToken(foreignToken) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `만료된 액세스 토큰은 거부한다`() {
        val expiredProvider = OgonggoTokenProvider(
            properties(accessTokenValidity = Duration.ofMinutes(30)),
            Clock.fixed(clock.instant().minus(Duration.ofHours(2)), ZoneId.of("Asia/Seoul")),
        )
        // 2시간 전에 발급된 30분짜리 토큰
        val expiredToken = expiredProvider.createAccessToken(17L)

        val exception = assertThrows(UnauthorizedException::class.java) { provider.parseAccessToken(expiredToken) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `형식이 잘못된 토큰은 거부한다`() {
        val exception = assertThrows(UnauthorizedException::class.java) { provider.parseAccessToken("not-a-jwt") }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    private fun properties(
        secret: String = SECRET,
        accessTokenValidity: Duration = Duration.ofMinutes(30),
    ): JwtProperties = JwtProperties(
        secret = secret,
        accessTokenValidity = accessTokenValidity,
        refreshTokenValidity = Duration.ofDays(14),
    )

    companion object {
        private const val SECRET =
            "b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA=="
        private const val OTHER_SECRET =
            "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdC1vbmx5LWRvLW5vdC11c2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMDAw"
    }
}
