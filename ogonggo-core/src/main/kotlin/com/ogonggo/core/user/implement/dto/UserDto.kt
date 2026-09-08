package com.ogonggo.core.user.implement.dto

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import java.time.LocalDateTime

data class UserAppendDto(
    val letsCareerUserId: Long,
    val joinedAt: LocalDateTime,
)

data class CompanyAccountAppendDto(
    val email: String,
    val encodedPassword: String,
    val joinedAt: LocalDateTime,
)

data class UserProfileSyncDto(
    val userId: Long,
    val name: String?,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val letsCareerUpdatedAt: LocalDateTime?,
    val syncedAt: LocalDateTime,
)

data class CompanyProfileAppendDto(
    val userId: Long,
    val organizationName: String,
    val managerName: String,
)

/**
 * 사용자가 오공고에서 직접 입력하는 학력과 희망 조건이다.
 * 렛츠커리어에서 복제하는 `UserProfileSyncDto`와 소유자가 달라 명령을 나눈다.
 */
data class UserProfileJobInfoDto(
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
)

data class UserProfileDto(
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
        internal fun from(profile: UserProfile): UserProfileDto = UserProfileDto(
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

data class CompanyProfileDto(
    val organizationName: String,
    val managerName: String,
) {
    companion object {
        internal fun from(profile: CompanyProfile): CompanyProfileDto = CompanyProfileDto(
            organizationName = profile.organizationName,
            managerName = profile.managerName,
        )
    }
}

/**
 * 기업 회원 로그인 검증에만 사용한다. 인코딩된 비밀번호를 담으므로 조회 결과를 응답에 그대로 노출하지 않는다.
 */
/**
 * User 엔티티는 core 내부 구현이므로 API 모듈에는 이 값 타입으로 노출한다.
 */
data class UserAccountDto(
    val userId: Long,
    val letsCareerUserId: Long?,
    val email: String?,
    val status: UserStatus,
    val role: UserRole,
    val joinedAt: LocalDateTime,
)
data class UserCredentialDto(
    val userId: Long,
    val encodedPassword: String,
    val status: UserStatus,
    val role: UserRole,
)
