package com.ogonggo.userapi.user.presentation.response

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

data class MyProfileResponse(
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        internal fun from(result: MyProfileResult): MyProfileResponse = MyProfileResponse(
            name = result.name,
            nickname = result.nickname,
            profileImageUrl = result.profileImageUrl,
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
