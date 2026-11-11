package com.ogonggo.userapi.auth.implement

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 비밀번호 찾기는 이메일만 알면 누구나 부를 수 있어 남의 비밀번호를 계속 바꿀 수 있다.
 * 같은 이메일은 [COOLDOWN]에 한 번만 재설정한다. 계정이 없는 이메일도 같이 막아 응답으로 계정 유무가 드러나지 않게 한다.
 */
@Component
class CompanyPasswordResetThrottle(
    private val redisTemplate: StringRedisTemplate,
) {

    /** 이번 요청을 처리해도 되면 true다. 처리하기로 한 순간 쿨다운을 건다. */
    fun tryAcquire(email: String): Boolean =
        redisTemplate.opsForValue().setIfAbsent(key(email), "1", COOLDOWN) == true

    private fun key(email: String): String = "$KEY_PREFIX${email.trim().lowercase()}"

    companion object {
        val COOLDOWN: Duration = Duration.ofMinutes(5)
        private const val KEY_PREFIX = "ogonggo:company-password-reset:"
    }
}
