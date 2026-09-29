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
    description = "이름·휴대폰 번호·닉네임·프로필 이미지는 렛츠커리어가 소유해 로그인마다 갱신되고 오공고에서 바꿀 수 없다. " +
        "학력과 희망 조건은 PUT /api/v1/users/me/profile, 수신 이메일은 PUT /api/v1/users/me/notification-email로 고친다.",
)
data class MyProfileResponse(
    val name: String?,
    @Schema(description = "렛츠커리어에 등록된 휴대폰 번호. 조회만 한다.", example = "010-1234-5678")
    val phoneNum: String?,
    @Schema(description = "오늘의 공고 정보를 받을 이메일. 가입 이메일과 따로 두며 입력하지 않았으면 null이다.")
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
) {
    companion object {
        internal fun from(result: MyProfileResult): MyProfileResponse = MyProfileResponse(
            name = result.name,
            phoneNum = result.phoneNum,
            notificationEmail = result.notificationEmail,
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
    @Schema(description = "기업 로고 이미지 주소. 입력하지 않았으면 null이다.")
    val logoUrl: String?,
    @Schema(description = "담당자 연락처. 입력하지 않았으면 null이다.")
    val managerPhone: String?,
    @Schema(description = "로그인 이메일과 따로 받는 정보 수신용 이메일. 입력하지 않았으면 null이다.")
    val notificationEmail: String?,
) {
    companion object {
        internal fun from(result: MyCompanyProfileResult): MyCompanyProfileResponse = MyCompanyProfileResponse(
            organizationName = result.organizationName,
            managerName = result.managerName,
            logoUrl = result.logoUrl,
            managerPhone = result.managerPhone,
            notificationEmail = result.notificationEmail,
        )
    }
}
