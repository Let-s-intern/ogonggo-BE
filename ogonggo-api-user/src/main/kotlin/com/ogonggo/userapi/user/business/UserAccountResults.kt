package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.CompanyProfileData
import com.ogonggo.core.user.implement.UserAccount
import com.ogonggo.core.user.implement.UserProfileData
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
    val profile: MyProfileResult?,
    val companyProfile: MyCompanyProfileResult?,
) {
    companion object {
        internal fun from(
            account: UserAccount,
            profile: UserProfileData?,
            companyProfile: CompanyProfileData?,
        ): MyAccountResult = MyAccountResult(
            userId = account.userId,
            role = account.role,
            status = account.status,
            // 기업 회원은 users.email로 로그인하고 일반 회원은 렛츠커리어 프로필의 이메일만 가진다.
            email = account.email ?: profile?.email,
            joinedAt = account.joinedAt,
            profile = profile?.let {
                MyProfileResult(
                    name = it.name,
                    nickname = it.nickname,
                    profileImageUrl = it.profileImageUrl,
                )
            },
            companyProfile = companyProfile?.let {
                MyCompanyProfileResult(
                    organizationName = it.organizationName,
                    managerName = it.managerName,
                )
            },
        )
    }
}

data class MyProfileResult(
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
)

data class MyCompanyProfileResult(
    val organizationName: String,
    val managerName: String,
)
