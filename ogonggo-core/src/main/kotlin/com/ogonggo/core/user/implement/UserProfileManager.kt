package com.ogonggo.core.user.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import java.time.LocalDateTime
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class UserProfileManager internal constructor(
    private val userProfileRepository: UserProfileJpaRepository,
) {

    /**
     * 프로필이 없으면 만들고, 있으면 렛츠커리어의 최종 수정 일시가 달라졌을 때만 갱신한다.
     * 로그인마다 무조건 UPDATE 하지 않기 위해 letscareer_updated_at 을 비교 기준으로 사용한다.
     */
    fun sync(command: UserProfileSyncDto) {
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

    /**
     * 렛츠커리어에서 복제하는 값과 달리 이 값들은 오공고가 소유하므로 `sync`가 건드리지 않는다.
     * 그래서 재로그인해도 여기서 저장한 값이 남는다.
     *
     * 렛츠커리어를 거치지 않은 계정은 프로필 행이 없을 수 있어 없으면 만든다.
     * 그 사이 다른 요청이 먼저 만들었으면 유니크 제약이 막는데,
     * 제약 위반은 트랜잭션을 롤백 대상으로 만들어 같은 트랜잭션에서 이어갈 수 없으므로
     * 조용히 삼키지 않고 재시도할 수 있는 충돌로 알린다.
     */
    fun replaceJobInfo(userId: Long, command: UserProfileJobInfoDto, now: LocalDateTime) {
        val profile = userProfileRepository.findByUserId(userId)
            ?: return createWithJobInfo(userId, command, now)

        profile.replaceJobInfo(
            university = command.university,
            major = command.major,
            grade = command.grade,
            wishField = command.wishField,
            wishJob = command.wishJob,
            wishIndustry = command.wishIndustry,
            wishEmploymentType = command.wishEmploymentType,
            wishCompany = command.wishCompany,
        )
        userProfileRepository.save(profile)
    }

    private fun createWithJobInfo(userId: Long, command: UserProfileJobInfoDto, now: LocalDateTime) {
        val created = UserProfile(userId = userId, lastSyncedAt = now).apply {
            replaceJobInfo(
                university = command.university,
                major = command.major,
                grade = command.grade,
                wishField = command.wishField,
                wishJob = command.wishJob,
                wishIndustry = command.wishIndustry,
                wishEmploymentType = command.wishEmploymentType,
                wishCompany = command.wishCompany,
            )
        }

        try {
            userProfileRepository.saveAndFlush(created)
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(UserErrorCode.USER_PROFILE_CONFLICT)
        }
    }
}
