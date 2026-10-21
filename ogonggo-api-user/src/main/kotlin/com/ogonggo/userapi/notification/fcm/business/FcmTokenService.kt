package com.ogonggo.userapi.notification.fcm.business

import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class FcmTokenService(
    private val userReader: UserReader,
    private val tokenReader: UserFcmTokenReader,
    private val tokenManager: UserFcmTokenManager,
    private val clock: Clock,
) {

    @Transactional
    fun replace(userId: Long, command: UserFcmTokenRegisterDto) {
        userReader.read(userId).status.requireActive()
        tokenManager.register(userId, command, LocalDateTime.now(clock))
    }

    fun get(userId: Long): String? {
        userReader.read(userId).status.requireActive()
        return tokenReader.readLatest(userId)
    }

    @Transactional
    fun delete(userId: Long, token: String) {
        userReader.read(userId).status.requireActive()
        tokenManager.revoke(userId, token, LocalDateTime.now(clock))
    }
}
