package com.ogonggo.userapi.auth.implement

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

interface RefreshTokenStore {
    fun save(userId: Long, refreshToken: String, validity: Duration)
    fun matches(userId: Long, refreshToken: String): Boolean
    fun delete(userId: Long)
}

/**
 * 사용자당 리프레시 토큰 하나만 유지한다. 새로 로그인하면 이전 토큰은 덮어써져 무효가 된다.
 * 로그아웃은 이 값을 지워 재발급을 막지만, 이미 발급된 액세스 토큰은 만료까지 유효하다.
 */
@Component
internal class RedisRefreshTokenStore(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenStore {

    override fun save(userId: Long, refreshToken: String, validity: Duration) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, validity)
    }

    override fun matches(userId: Long, refreshToken: String): Boolean =
        redisTemplate.opsForValue().get(key(userId)) == refreshToken

    override fun delete(userId: Long) {
        redisTemplate.delete(key(userId))
    }

    private fun key(userId: Long): String = "$KEY_PREFIX$userId"

    companion object {
        private const val KEY_PREFIX = "ogonggo:refresh:"
    }
}
