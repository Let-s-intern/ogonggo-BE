package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.springframework.stereotype.Component

interface UserProfileManager {
    fun sync(command: UserProfileSyncCommand)
}

@Component
internal class UserProfileManagerImpl(
    private val userProfileRepository: UserProfileJpaRepository,
) : UserProfileManager {

    /**
     * 프로필이 없으면 만들고, 있으면 렛츠커리어의 최종 수정 일시가 달라졌을 때만 갱신한다.
     * 로그인마다 무조건 UPDATE 하지 않기 위해 letscareer_updated_at 을 비교 기준으로 사용한다.
     */
    override fun sync(command: UserProfileSyncCommand) {
        val profile = userProfileRepository.findByUserId(command.userId)

        if (profile == null) {
            userProfileRepository.save(
                UserProfile(
                    userId = command.userId,
                    name = command.name,
                    email = command.email,
                    nickname = command.nickname,
                    profileImageUrl = command.profileImageUrl,
                    letsCareerUpdatedAt = command.letsCareerUpdatedAt,
                    lastSyncedAt = command.syncedAt,
                ),
            )
            return
        }

        if (profile.letsCareerUpdatedAt == command.letsCareerUpdatedAt) {
            return
        }

        profile.sync(
            name = command.name,
            email = command.email,
            nickname = command.nickname,
            profileImageUrl = command.profileImageUrl,
            letsCareerUpdatedAt = command.letsCareerUpdatedAt,
            syncedAt = command.syncedAt,
        )
        userProfileRepository.save(profile)
    }
}
