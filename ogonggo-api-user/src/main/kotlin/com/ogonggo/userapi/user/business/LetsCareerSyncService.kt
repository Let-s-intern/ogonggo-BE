package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class LetsCareerSyncService(
    private val userReader: UserReader,
    private val userProfileManager: UserProfileManager,
    private val clock: Clock,
) {

    /**
     * 렛츠커리어에서 고친 학력·희망 조건을 나중에 고친 쪽 기준으로 반영한다. 반영했으면 true다.
     * 오공고 계정이 없는 렛츠커리어 사용자는 받을 곳이 없으므로 반영하지 않는다. 처음 로그인할 때 최신 값을 복제한다.
     * 렛츠커리어에서 온 값이므로 렛츠커리어로 다시 보낼 변경을 적재하지 않는다.
     */
    @Transactional
    fun applyJobProfile(letsCareerUserId: Long, command: UserProfileJobInfoDto, letsCareerUpdatedAt: LocalDateTime?): Boolean {
        val account = userReader.readByLetsCareerUserId(letsCareerUserId) ?: return false
        return userProfileManager.applyLetsCareerJobInfo(
            userId = account.userId,
            command = command,
            letsCareerUpdatedAt = letsCareerUpdatedAt,
            now = LocalDateTime.now(clock),
        )
    }
}
