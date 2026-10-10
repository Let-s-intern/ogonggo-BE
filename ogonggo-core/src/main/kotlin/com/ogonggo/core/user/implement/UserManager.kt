package com.ogonggo.core.user.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
