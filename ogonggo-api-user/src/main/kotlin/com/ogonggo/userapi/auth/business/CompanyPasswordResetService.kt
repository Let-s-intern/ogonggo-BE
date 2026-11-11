package com.ogonggo.userapi.auth.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.CompanyTemporaryPasswordGenerator
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.implement.CompanyPasswordResetThrottle
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 비즈니스 회원 비밀번호 찾기. 비밀번호를 임시 비밀번호로 바로 바꾸고 로그인 이메일로 보낸다.
 *
 * 계정이 없거나 정지·탈퇴했어도 같은 성공 응답을 준다. 다르게 응답하면 이 API가 계정 존재 확인 수단이 된다.
 * 바꾼 뒤에는 리프레시 토큰을 지워 다른 기기의 세션을 끊는다.
 */
@Service
class CompanyPasswordResetService(
    private val userReader: UserReader,
    private val userManager: UserManager,
    private val companyProfileReader: CompanyProfileReader,
    private val temporaryPasswordGenerator: CompanyTemporaryPasswordGenerator,
    private val passwordEncoder: PasswordEncoder,
    private val throttle: CompanyPasswordResetThrottle,
    private val refreshTokenStore: RefreshTokenStore,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    fun reset(email: String) {
        if (!throttle.tryAcquire(email)) {
            return
        }
        val account = userReader.readByEmail(email)
        if (account == null || account.role != UserRole.COMPANY || account.status != UserStatus.ACTIVE) {
            return
        }

        val temporaryPassword = temporaryPasswordGenerator.generate()
        val loginEmail = checkNotNull(account.email)
        transactionTemplate.executeWithoutResult {
            userManager.changePassword(account.userId, checkNotNull(passwordEncoder.encode(temporaryPassword)))
            notificationAppender.append(
                NotificationAppendDto(
                    deduplicationKey = "$TEMPLATE_CODE:user:${account.userId}:${UUID.randomUUID()}",
                    channel = NotificationChannel.EMAIL,
                    templateCode = TEMPLATE_CODE,
                    recipientAddress = loginEmail,
                    payloadJson = objectMapper.writeValueAsString(
                        mapOf(
                            "managerName" to companyProfileReader.read(account.userId)?.managerName.orEmpty(),
                            "email" to loginEmail,
                            "temporaryPassword" to temporaryPassword,
                        ),
                    ),
                    scheduledAt = LocalDateTime.now(clock),
                    recipientUserId = account.userId,
                ),
            )
        }
        refreshTokenStore.delete(account.userId)
        log.info("비즈니스 회원 비밀번호를 임시 비밀번호로 바꿨습니다. userId={}", account.userId)
    }

    private companion object {
        /** EmailTemplate.COMPANY_PASSWORD_RESET가 처리한다. */
        const val TEMPLATE_CODE = "company_password_reset"
        val log = LoggerFactory.getLogger(CompanyPasswordResetService::class.java)
    }
}
