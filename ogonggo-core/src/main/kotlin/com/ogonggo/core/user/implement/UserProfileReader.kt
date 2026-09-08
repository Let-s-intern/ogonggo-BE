package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.springframework.stereotype.Component

@Component
class UserProfileReader internal constructor(
    private val userProfileRepository: UserProfileJpaRepository,
) {

    /** 일반 회원의 프로필을 읽는다. 기업 회원에게는 프로필 행이 없으므로 null을 반환한다. */

    fun read(userId: Long): UserProfileData? =
        userProfileRepository.findByUserId(userId)?.let(UserProfileData::from)
}

data class UserProfileData(
    val name: String?,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
) {
    companion object {
        internal fun from(profile: UserProfile): UserProfileData = UserProfileData(
            name = profile.name,
            email = profile.email,
            nickname = profile.nickname,
            profileImageUrl = profile.profileImageUrl,
            university = profile.university,
            major = profile.major,
            grade = profile.grade,
            wishField = profile.wishField,
            wishJob = profile.wishJob,
            wishIndustry = profile.wishIndustry,
            wishEmploymentType = profile.wishEmploymentType,
            wishCompany = profile.wishCompany,
        )
    }
}
