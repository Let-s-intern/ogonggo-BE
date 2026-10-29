package com.ogonggo.adminapi.member.presentation.response

import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.CompanyEmployeeCountRange
import com.ogonggo.core.user.domain.CompanyManagerDuty
import com.ogonggo.core.user.domain.CompanyType
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserInflowSource
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.dto.CompanyMemberDto
import com.ogonggo.core.user.implement.dto.GeneralMemberDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 프로필이 아직 없는 회원은 프로필 칸이 모두 null이다. */
data class AdminGeneralMemberResponse(
    val userId: Long,
    @Schema(description = "렛츠커리어 사용자 식별자")
    val letsCareerUserId: Long?,
    @Schema(description = "USER·MENTOR·ADMIN 중 하나입니다.")
    val role: UserRole,
    val status: UserStatus,
    val joinedAt: LocalDateTime,
    @Schema(description = "탈퇴 일시. 탈퇴하지 않았으면 null입니다.")
    val withdrawnAt: LocalDateTime?,
    @Schema(description = "유입 구분. 렛츠커리어를 쓰다 넘어왔으면 LETSCAREER, 오공고로 처음 들어왔으면 OGONGGO입니다. 아직 정하지 못한 회원은 null입니다.")
    val inflowSource: UserInflowSource?,
    val name: String?,
    val nickname: String?,
    @Schema(description = "렛츠커리어에서 받은 이메일")
    val email: String?,
    val profileImageUrl: String?,
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    @Schema(description = "희망 직군")
    val wishField: String?,
    @Schema(description = "희망 직무")
    val wishJob: String?,
    @Schema(description = "희망 산업")
    val wishIndustry: String?,
    @Schema(description = "희망 구직 조건")
    val wishEmploymentType: String?,
    @Schema(description = "희망 기업")
    val wishCompany: String?,
) {
    companion object {
        internal fun from(member: GeneralMemberDto): AdminGeneralMemberResponse {
            val profile = member.profile
            return AdminGeneralMemberResponse(
                userId = member.userId,
                letsCareerUserId = member.letsCareerUserId,
                role = member.role,
                status = member.status,
                joinedAt = member.joinedAt,
                withdrawnAt = member.withdrawnAt,
                inflowSource = member.inflowSource,
                name = profile?.name,
                nickname = profile?.nickname,
                email = profile?.email,
                profileImageUrl = profile?.profileImageUrl,
                university = profile?.university,
                major = profile?.major,
                grade = profile?.grade,
                wishField = profile?.wishField,
                wishJob = profile?.wishJob,
                wishIndustry = profile?.wishIndustry,
                wishEmploymentType = profile?.wishEmploymentType,
                wishCompany = profile?.wishCompany,
            )
        }
    }
}

data class AdminCompanyMemberResponse(
    val userId: Long,
    @Schema(description = "로그인 이메일")
    val email: String?,
    val status: UserStatus,
    val joinedAt: LocalDateTime,
    @Schema(description = "탈퇴 일시. 탈퇴하지 않았으면 null입니다.")
    val withdrawnAt: LocalDateTime?,
    @Schema(description = "회사명")
    val organizationName: String?,
    val managerName: String?,
    val managerPhone: String?,
    @Schema(description = "로그인 이메일과 따로 받는 정보 수신용 이메일")
    val notificationEmail: String?,
    val logoUrl: String?,
    @Schema(description = "사업자 번호 종류. 해외 기업이면 OVERSEAS입니다. 아래 가입 정보는 그 값을 받기 전에 가입한 회원이면 모두 null입니다.")
    val businessNumberType: CompanyBusinessNumberType?,
    @Schema(description = "하이픈 없는 사업자등록번호(10자리) 또는 법인등록번호(13자리). 진위는 확인하지 않았습니다.")
    val businessNumber: String?,
    val companyType: CompanyType?,
    val employeeCountRange: CompanyEmployeeCountRange?,
    @Schema(description = "산업")
    val industry: String?,
    val address: String?,
    val homepageUrl: String?,
    @Schema(description = "담당자 직책")
    val managerTitle: String?,
    @Schema(description = "담당 업무")
    val managerDuty: CompanyManagerDuty?,
    @Schema(description = "추천인")
    val recommender: String?,
) {
    companion object {
        internal fun from(member: CompanyMemberDto): AdminCompanyMemberResponse {
            val companyProfile = member.companyProfile
            return AdminCompanyMemberResponse(
                userId = member.userId,
                email = member.email,
                status = member.status,
                joinedAt = member.joinedAt,
                withdrawnAt = member.withdrawnAt,
                organizationName = companyProfile?.organizationName,
                managerName = companyProfile?.managerName,
                managerPhone = companyProfile?.managerPhone,
                notificationEmail = companyProfile?.notificationEmail,
                logoUrl = companyProfile?.logoUrl,
                businessNumberType = companyProfile?.businessNumberType,
                businessNumber = companyProfile?.businessNumber,
                companyType = companyProfile?.companyType,
                employeeCountRange = companyProfile?.employeeCountRange,
                industry = companyProfile?.industry,
                address = companyProfile?.address,
                homepageUrl = companyProfile?.homepageUrl,
                managerTitle = companyProfile?.managerTitle,
                managerDuty = companyProfile?.managerDuty,
                recommender = companyProfile?.recommender,
            )
        }
    }
}
