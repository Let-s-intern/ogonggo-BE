package com.ogonggo.userapi.notification.fcm.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class FcmTokenServiceTest {
    private val userReader = Mockito.mock(UserReader::class.java)
    private val tokenReader = Mockito.mock(UserFcmTokenReader::class.java)
    private val tokenManager = Mockito.mock(UserFcmTokenManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-21T11:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = FcmTokenService(userReader, tokenReader, tokenManager, clock)

    @Test
    fun `정지 탈퇴 회원은 토큰 등록 조회 해제를 할 수 없다`() {
        // given
        listOf(UserStatus.SUSPENDED to "USER_SUSPENDED", UserStatus.WITHDRAWN to "USER_WITHDRAWN")
            .forEach { (status, code) ->
                Mockito.`when`(userReader.read(17L)).thenReturn(
                    UserAccountDto(17L, 1L, null, status, UserRole.USER, LocalDateTime.now(clock)),
                )
                // when, then
                val actions = listOf<() -> Unit>(
                    { service.replace(17L, UserFcmTokenRegisterDto("token", null)) },
                    { service.get(17L) },
                    { service.delete(17L, "token") },
                )
                actions.forEach { action ->
                    assertEquals(code, assertThrows<ForbiddenException> { action() }.errorCode.code)
                }
            }
        Mockito.verifyNoInteractions(tokenReader, tokenManager)
    }
}
