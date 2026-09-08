package com.ogonggo.userapi.user.presentation.response

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.userapi.user.business.MyAccountResult
import com.ogonggo.userapi.user.business.MyCompanyProfileResult
import com.ogonggo.userapi.user.business.MyProfileResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class MyAccountResponse(
    val userId: Long,
    val role: UserRole,
    val status: UserStatus,
    @Schema(description = "기업 회원은 로그인 이메일, 일반 회원은 렛츠커리어 프로필의 이메일이다.")
    val email: String?,
    val joinedAt: LocalDateTime,
    @Schema(description = "일반 회원의 렛츠커리어 프로필. 기업 회원이면 null이다.")
    val profile: MyProfileResponse?,
    @Schema(description = "기업 회원의 기업 정보. 일반 회원이면 null이다.")
    val companyProfile: MyCompanyProfileResponse?,
) {
    companion object {
        internal fun from(result: MyAccountResult): MyAccountResponse = MyAccountResponse(
            userId = result.userId,
            role = result.role,
            status = result.status,
            email = result.email,
            joinedAt = result.joinedAt,
            profile = result.profile?.let(MyProfileResponse::from),
            companyProfile = result.companyProfile?.let(MyCompanyProfileResponse::from),
        )
    }
}

@Schema(
    description = "이름·닉네임·프로필 이미지는 렛츠커리어가 소유해 로그인마다 갱신되고, " +
        "학력과 희망 조건은 오공고가 소유해 PUT /api/v1/users/me/profile로 고친다.",
)
data class MyProfileResponse(
    val name: String?,
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
        internal fun from(result: MyProfileResult): MyProfileResponse = MyProfileResponse(
            name = result.name,
            nickname = result.nickname,
            profileImageUrl = result.profileImageUrl,
            university = result.university,
            major = result.major,
            grade = result.grade,
            wishField = result.wishField,
            wishJob = result.wishJob,
            wishIndustry = result.wishIndustry,
            wishEmploymentType = result.wishEmploymentType,
            wishCompany = result.wishCompany,
        )
    }
}

data class MyCompanyProfileResponse(
    val organizationName: String,
    val managerName: String,
) {
    companion object {
        internal fun from(result: MyCompanyProfileResult): MyCompanyProfileResponse = MyCompanyProfileResponse(
            organizationName = result.organizationName,
            managerName = result.managerName,
        )
    }
}
