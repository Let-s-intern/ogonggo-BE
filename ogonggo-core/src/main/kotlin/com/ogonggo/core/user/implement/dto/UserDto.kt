package com.ogonggo.core.user.implement.dto

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
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
    val phoneNum: String?,
    val letsCareerAuthProvider: LetsCareerAuthProvider?,
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

/** 기업 회원이 마이페이지에서 고치는 기본 정보다. 함께 교체하므로 로고가 null이면 지운다. */
data class CompanyBasicInfoUpdateDto(
    val organizationName: String,
    val logo: CompanyLogoDto?,
)

/** 기업 로고로 연결한 이미지다. 식별자는 이미지를 바꾸거나 지울 때 이전 이미지를 정리하는 데 쓴다. */
data class CompanyLogoDto(
    val imageId: String,
    val url: String,
)

/** 기업 회원이 마이페이지에서 고치는 담당자 정보다. 함께 교체하므로 선택 값이 null이면 비운다. */
data class CompanyManagerInfoUpdateDto(
    val managerName: String,
    val managerPhone: String?,
    val notificationEmail: String?,
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
    val phoneNum: String?,
    val letsCareerAuthProvider: LetsCareerAuthProvider?,
    val notificationEmail: String?,
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
    val jobInfoUpdatedAt: LocalDateTime? = null,
) {
    companion object {
        internal fun from(profile: UserProfile): UserProfileDto = UserProfileDto(
            name = profile.name,
            email = profile.email,
            phoneNum = profile.phoneNum,
            letsCareerAuthProvider = profile.letsCareerAuthProvider,
            notificationEmail = profile.notificationEmail,
            nickname = profile.nickname,
            // 오공고에서 올린 이미지가 있으면 그것을, 없으면 렛츠커리어 이미지를 보인다.
            profileImageUrl = profile.ogonggoProfileImageUrl ?: profile.profileImageUrl,
            university = profile.university,
            major = profile.major,
            grade = profile.grade,
            wishField = profile.wishField,
            wishJob = profile.wishJob,
            wishIndustry = profile.wishIndustry,
            wishEmploymentType = profile.wishEmploymentType,
            wishCompany = profile.wishCompany,
            jobInfoUpdatedAt = profile.jobInfoUpdatedAt,
        )
    }
}

/** 렛츠커리어로 보낼 학력·희망 조건 변경 한 건이다. */
data class LetsCareerJobProfileOutboxDto(
    val userId: Long,
    val requestedAt: LocalDateTime,
    val attemptCount: Int,
)

data class CompanyProfileDto(
    val organizationName: String,
    val managerName: String,
    val logoUrl: String?,
    val managerPhone: String?,
    val notificationEmail: String?,
    val logoImageId: String? = null,
) {
    companion object {
        internal fun from(profile: CompanyProfile): CompanyProfileDto = CompanyProfileDto(
            organizationName = profile.organizationName,
            managerName = profile.managerName,
            logoUrl = profile.logoUrl,
            logoImageId = profile.logoImageId,
            managerPhone = profile.managerPhone,
            notificationEmail = profile.notificationEmail,
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

/**
 * 관리자 콘솔이 보는 일반 회원이다.
 * 렛츠커리어 프로필을 받기 전에 만들어진 계정은 프로필 행이 없어 `profile`이 null일 수 있다.
 */
data class GeneralMemberDto(
    val userId: Long,
    val letsCareerUserId: Long?,
    val status: UserStatus,
    val joinedAt: LocalDateTime,
    val withdrawnAt: LocalDateTime?,
    val profile: UserProfileDto?,
) {
    companion object {
        internal fun of(user: User, profile: UserProfile?): GeneralMemberDto = GeneralMemberDto(
            userId = checkNotNull(user.id) { "사용자 식별자가 없습니다." },
            letsCareerUserId = user.letsCareerUserId,
            status = user.status,
            joinedAt = user.joinedAt,
            withdrawnAt = user.withdrawnAt,
            profile = profile?.let(UserProfileDto::from),
        )
    }
}

/**
 * 관리자 콘솔이 보는 기업 회원이다.
 * 기업 정보는 가입과 같은 트랜잭션에서 만들어지지만 조회 화면이 데이터 어긋남으로 깨지지 않도록 null을 허용한다.
 */
data class CompanyMemberDto(
    val userId: Long,
    val email: String?,
    val status: UserStatus,
    val joinedAt: LocalDateTime,
    val withdrawnAt: LocalDateTime?,
    val companyProfile: CompanyProfileDto?,
) {
    companion object {
        internal fun of(user: User, companyProfile: CompanyProfile?): CompanyMemberDto = CompanyMemberDto(
            userId = checkNotNull(user.id) { "사용자 식별자가 없습니다." },
            email = user.email,
            status = user.status,
            joinedAt = user.joinedAt,
            withdrawnAt = user.withdrawnAt,
            companyProfile = companyProfile?.let(CompanyProfileDto::from),
        )
    }
}

data class GeneralMemberPageDto(
    val members: List<GeneralMemberDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class CompanyMemberPageDto(
    val members: List<CompanyMemberDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
