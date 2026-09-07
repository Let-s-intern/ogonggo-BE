package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.springframework.stereotype.Component

interface UserProfileReader {
    /** 일반 회원의 프로필을 읽는다. 기업 회원에게는 프로필 행이 없으므로 null을 반환한다. */
    fun read(userId: Long): UserProfileData?
}

@Component
internal class UserProfileReaderImpl(
    private val userProfileRepository: UserProfileJpaRepository,
) : UserProfileReader {

    override fun read(userId: Long): UserProfileData? =
        userProfileRepository.findByUserId(userId)?.let(UserProfileData::from)
}

data class UserProfileData(
    val name: String?,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        internal fun from(profile: UserProfile): UserProfileData = UserProfileData(
            name = profile.name,
            email = profile.email,
            nickname = profile.nickname,
            profileImageUrl = profile.profileImageUrl,
        )
    }
}
