package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.dto.CompanyProfileDto
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import java.time.LocalDateTime

/**
 * 로그인한 사용자가 자기 자신을 볼 때의 결과다.
 * 계정 종류에 따라 채워지는 프로필이 다르므로 두 프로필을 모두 두고 해당하지 않는 쪽은 null로 둔다.
 */
data class MyAccountResult(
    val userId: Long,
    val role: UserRole,
    val status: UserStatus,
    val email: String?,
    val joinedAt: LocalDateTime,
    val passwordChangeable: Boolean,
    val profile: MyProfileResult?,
    val companyProfile: MyCompanyProfileResult?,
) {
    companion object {
        internal fun from(
            account: UserAccountDto,
            profile: UserProfileDto?,
            companyProfile: CompanyProfileDto?,
        ): MyAccountResult = MyAccountResult(
            userId = account.userId,
            role = account.role,
            status = account.status,
            // 기업 회원은 users.email로 로그인하고 일반 회원은 렛츠커리어 프로필의 이메일만 가진다.
            email = account.email ?: profile?.email,
            joinedAt = account.joinedAt,
            // 기업 회원은 오공고가 비밀번호를 갖는다. 일반 회원은 소셜 가입이 확인된 경우만 막고,
            // 가입 경로를 아직 모르면 렛츠커리어 판정에 맡긴다.
            passwordChangeable = profile?.letsCareerAuthProvider?.hasPassword ?: true,
            profile = profile?.let(MyProfileResult::from),
            companyProfile = companyProfile?.let {
                MyCompanyProfileResult(
                    organizationName = it.organizationName,
                    managerName = it.managerName,
                    logoImageId = it.logoImageId,
                    logoUrl = it.logoUrl,
                    managerPhone = it.managerPhone,
                    notificationEmail = it.notificationEmail,
                )
            },
        )
    }
}

/**
 * 렛츠커리어에서 복제한 값과 오공고에서 직접 입력한 값이 함께 담긴다.
 * 이름·휴대폰 번호·닉네임은 로그인마다 렛츠커리어 값으로 갱신되고, 나머지는 사용자가 고친다.
 * 프로필 이미지는 오공고에서 바꾼 이미지가 있으면 그것을, 없으면 렛츠커리어 이미지를 담는다.
 */
data class MyProfileResult(
    val name: String?,
    val phoneNum: String?,
    val authProvider: LetsCareerAuthProvider?,
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
        internal fun from(profile: UserProfileDto): MyProfileResult = MyProfileResult(
            name = profile.name,
            phoneNum = profile.phoneNum,
            authProvider = profile.letsCareerAuthProvider,
            notificationEmail = profile.notificationEmail,
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

data class MyCompanyProfileResult(
    val organizationName: String,
    val managerName: String,
    val logoImageId: String?,
    val logoUrl: String?,
    val managerPhone: String?,
    val notificationEmail: String?,
)

/** 기업 회원이 고치는 기본 정보다. 로고는 업로드한 이미지 식별자로 받으며 null이면 지운다. */
data class CompanyBasicInfoCommand(
    val organizationName: String,
    val logoImageId: String?,
)
