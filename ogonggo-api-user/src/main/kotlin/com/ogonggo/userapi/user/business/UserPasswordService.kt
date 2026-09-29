package com.ogonggo.userapi.user.business

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class UserPasswordService(
    private val userReader: UserReader,
    private val userManager: UserManager,
    private val letsCareerUserClient: LetsCareerUserClient,
    private val passwordEncoder: PasswordEncoder,
    private val transactionTemplate: TransactionTemplate,
) {

    /**
     * 비밀번호를 가진 쪽에서 바꾼다. 일반 회원은 렛츠커리어가, 기업 회원은 오공고가 소유한다.
     *
     * 렛츠커리어 호출은 트랜잭션 밖에서 한다. 응답을 기다리는 동안 DB 커넥션을 잡지 않기 위해서다.
     */
    fun changeMyPassword(userId: Long, command: PasswordChangeCommand) {
        val letsCareerUserId = userReader.read(userId).letsCareerUserId
        if (letsCareerUserId != null) {
            letsCareerUserClient.changePassword(letsCareerUserId, command.currentPassword, command.newPassword)
            return
        }

        transactionTemplate.executeWithoutResult { changeCompanyPassword(userId, command) }
    }

    private fun changeCompanyPassword(userId: Long, command: PasswordChangeCommand) {
        val credential = checkNotNull(userReader.readCredential(userId)) {
            "렛츠커리어 계정도 비밀번호도 없는 사용자입니다. userId=$userId"
        }
        if (!passwordEncoder.matches(command.currentPassword, credential.encodedPassword)) {
            throw InvalidValueException(UserErrorCode.CURRENT_PASSWORD_MISMATCH)
        }

        userManager.changePassword(userId, checkNotNull(passwordEncoder.encode(command.newPassword)))
    }
}

data class PasswordChangeCommand(
    val currentPassword: String,
    val newPassword: String,
)
