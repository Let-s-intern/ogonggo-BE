package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserInflowSource
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class UserManager internal constructor(
    private val userRepository: UserJpaRepository,
) {

    /** 인코딩된 값만 받는다. 인코딩 방식은 API 모듈이 소유한다. */
    fun changePassword(userId: Long, encodedPassword: String) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)

        user.changePassword(encodedPassword)
        userRepository.save(user)
    }

    /** 같은 역할로 바꾸면 아무것도 바꾸지 않는다. 변경 이력은 남기지 않는다. */
    @Transactional
    fun changeRole(userId: Long, role: UserRole) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)

        if (user.letsCareerUserId == null) {
            throw InvalidValueException(UserErrorCode.COMPANY_MEMBER_ROLE_UNCHANGEABLE)
        }
        if (user.status == UserStatus.WITHDRAWN) {
            throw ConflictException(UserErrorCode.WITHDRAWN_USER_ROLE_UNCHANGEABLE)
        }

        user.changeRole(role)
    }

    /** 이미 정한 회원은 바꾸지 않는다. 로그인 트랜잭션 안에서 부른다. */
    @Transactional
    fun classifyInflowSource(userId: Long, knownInflowSource: UserInflowSource?, letsCareerJoinedAt: LocalDateTime?) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)

        user.classifyInflowSource(knownInflowSource, letsCareerJoinedAt)
    }

    @Transactional
    fun changeFcmToken(userId: Long, fcmToken: String) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)

        user.changeFcmToken(fcmToken)
        userRepository.save(user)
    }

    @Transactional
    fun clearFcmToken(userId: Long, expectedToken: String) {
        val user = userRepository.findByIdOrNull(userId)
            ?: return

        user.clearFcmToken(expectedToken)
        userRepository.save(user)
    }
}
