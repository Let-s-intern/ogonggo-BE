package com.ogonggo.userapi.notification.fcm.business

import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmTokenService(
    private val userReader: UserReader,
    private val userManager: UserManager,
) {

    @Transactional
    fun replace(userId: Long, token: String) {
        userManager.changeFcmToken(userId, token)
    }

    @Transactional(readOnly = true)
    fun get(userId: Long): String? = userReader.readFcmToken(userId)
}
