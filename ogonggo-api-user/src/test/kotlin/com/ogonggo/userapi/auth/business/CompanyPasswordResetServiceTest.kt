package com.ogonggo.userapi.auth.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.CompanyTemporaryPasswordGenerator
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.userapi.auth.implement.CompanyPasswordResetThrottle
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CompanyPasswordResetServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userManager = Mockito.mock(UserManager::class.java)
    private val companyProfileReader = Mockito.mock(CompanyProfileReader::class.java)
    private val generator = Mockito.mock(CompanyTemporaryPasswordGenerator::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val throttle = Mockito.mock(CompanyPasswordResetThrottle::class.java)
    private val refreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
    private val notificationAppender = Mockito.mock(NotificationAppender::class.java)
    private val transactionManager = Mockito.mock(PlatformTransactionManager::class.java).also {
        Mockito.`when`(it.getTransaction(Mockito.any())).thenReturn(SimpleTransactionStatus())
    }

    private val service = CompanyPasswordResetService(
        userReader = userReader,
        userManager = userManager,
        companyProfileReader = companyProfileReader,
        temporaryPasswordGenerator = generator,
        passwordEncoder = passwordEncoder,
        throttle = throttle,
        refreshTokenStore = refreshTokenStore,
        notificationAppender = notificationAppender,
        objectMapper = ObjectMapper(),
        transactionTemplate = TransactionTemplate(transactionManager),
        clock = Clock.fixed(Instant.parse("2026-11-11T03:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `활성 비즈니스 회원이면 임시 비밀번호로 바꾸고 메일을 적재하고 세션을 끊는다`() {
        // given
        Mockito.`when`(throttle.tryAcquire(EMAIL)).thenReturn(true)
        Mockito.`when`(userReader.readByEmail(EMAIL)).thenReturn(account(UserRole.COMPANY, UserStatus.ACTIVE))
        Mockito.`when`(generator.generate()).thenReturn("Ab3dEf7hJk9m")
        Mockito.`when`(passwordEncoder.encode("Ab3dEf7hJk9m")).thenReturn("encoded")

        // when
        service.reset(EMAIL)

        // then
        Mockito.verify(userManager).changePassword(USER_ID, "encoded")
        Mockito.verify(refreshTokenStore).delete(USER_ID)
        // Kotlin non-null 인자라 ArgumentCaptor 대신 기록된 호출에서 꺼낸다.
        val notification = Mockito.mockingDetails(notificationAppender).invocations.single().arguments[0] as NotificationAppendDto
        assertEquals("company_password_reset", notification.templateCode)
        assertEquals(EMAIL, notification.recipientAddress)
        assertEquals(USER_ID, notification.recipientUserId)
        assertEquals("Ab3dEf7hJk9m", ObjectMapper().readTree(notification.payloadJson)["temporaryPassword"].asText())
    }

    @Test
    fun `계정이 없거나 정지된 회원이거나 쿨다운 중이면 아무것도 바꾸지 않는다`() {
        Mockito.`when`(throttle.tryAcquire(EMAIL)).thenReturn(true)
        Mockito.`when`(userReader.readByEmail(EMAIL)).thenReturn(null)
        service.reset(EMAIL)

        Mockito.`when`(userReader.readByEmail(EMAIL)).thenReturn(account(UserRole.COMPANY, UserStatus.SUSPENDED))
        service.reset(EMAIL)

        Mockito.`when`(throttle.tryAcquire(EMAIL)).thenReturn(false)
        Mockito.`when`(userReader.readByEmail(EMAIL)).thenReturn(account(UserRole.COMPANY, UserStatus.ACTIVE))
        service.reset(EMAIL)

        Mockito.verifyNoInteractions(userManager, notificationAppender, refreshTokenStore, generator)
    }

    private fun account(role: UserRole, status: UserStatus) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = null,
        email = EMAIL,
        status = status,
        role = role,
        joinedAt = LocalDateTime.of(2026, 11, 1, 9, 0),
    )

    private companion object {
        const val USER_ID = 501L
        const val EMAIL = "ad@example.com"
    }
}
