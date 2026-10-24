package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.fcmtopicsynctask.implement.FcmTopicSyncTaskManager
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.error.UserFcmTokenErrorCode
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class UserFcmTokenManager internal constructor(
    private val tokenRepository: UserFcmTokenJpaRepository,
    private val userRepository: UserJpaRepository,
    private val topicSync: FcmTopicSyncTaskManager,
) {
    /** 호출한 Business Service의 트랜잭션에서 회원·기존 연결을 잠그고 갱신한다. */
    fun register(userId: Long, command: UserFcmTokenRegisterDto, now: LocalDateTime) {
        require(command.token.isNotBlank() && command.token.length <= 4096)
        try {
            userRepository.findForFcmTokenUpdate(userId)
                ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)
            val slot = UserFcmToken.slot(userId, command.deviceId)
            val bindings = tokenRepository.findBindingsForUpdate(slot, UserFcmToken.hash(command.token))
            val current = bindings.firstOrNull { it.userId == userId && it.activeSlot == slot }

            // 계정 전환과 동일 토큰의 다른 슬롯 연결을 해제한 뒤 unique 키를 재사용한다.
            bindings.filter { it !== current }.forEach {
                it.token?.let { old -> topicSync.request(old, false, now) }
                it.revoke(now)
            }
            current?.token?.takeIf { it != command.token }?.let { topicSync.request(it, false, now) }
            tokenRepository.flush()
            if (current == null) {
                tokenRepository.saveAndFlush(UserFcmToken(userId, command.deviceId?.toString(), command.token, slot))
            } else {
                current.replace(command.token, now)
                tokenRepository.flush()
            }
            topicSync.request(command.token, true, now)
        } catch (_: DataIntegrityViolationException) {
            // 아직 행이 없는 설치·토큰의 동시 등록은 DB 제약이 최종 방어한다. 전체 요청을 재시도한다.
            throw ConflictException(UserFcmTokenErrorCode.FCM_TOKEN_CONFLICT)
        } catch (_: PessimisticLockingFailureException) {
            throw ConflictException(UserFcmTokenErrorCode.FCM_TOKEN_CONFLICT)
        }
    }

    /** 비동기 sender에서도 호출한다. WHERE에서 소유자와 발송한 토큰을 비교해 새 토큰을 보호한다. */
    @Transactional
    fun revoke(userId: Long, expectedToken: String, now: LocalDateTime) {
        val changed = tokenRepository.revoke(userId, UserFcmToken.hash(expectedToken), now)
        if (changed > 0) topicSync.request(expectedToken, false, now)
    }
}
