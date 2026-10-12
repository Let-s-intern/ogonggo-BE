package com.ogonggo.userapi.user.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class UserNicknameService(
    private val userReader: UserReader,
    private val userProfileManager: UserProfileManager,
    private val letsCareerUserClient: LetsCareerUserClient,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    /**
     * 닉네임은 렛츠커리어가 소유하므로 렛츠커리어에서 먼저 바꾸고, 성공하면 오공고 프로필에도 바로 반영한다.
     * 렛츠커리어에서 실패하면 오공고 값도 바꾸지 않는다.
     *
     * 렛츠커리어 호출은 트랜잭션 밖에서 한다. 응답을 기다리는 동안 DB 커넥션을 잡지 않기 위해서다.
     */
    fun changeMyNickname(userId: Long, nickname: String) {
        val letsCareerUserId = userReader.read(userId).letsCareerUserId
            ?: throw ForbiddenException(UserErrorCode.GENERAL_MEMBER_REQUIRED)

        letsCareerUserClient.changeNickname(letsCareerUserId, nickname)
        transactionTemplate.executeWithoutResult {
            userProfileManager.changeNickname(userId, nickname, LocalDateTime.now(clock))
        }
    }
}
