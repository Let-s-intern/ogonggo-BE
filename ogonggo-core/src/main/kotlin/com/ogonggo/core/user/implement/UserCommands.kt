package com.ogonggo.core.user.implement

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import java.time.LocalDateTime

/**
 * User 엔티티는 core 내부 구현이므로 API 모듈에는 이 값 타입으로 노출한다.
 */
data class UserAccount(
    val userId: Long,
    val letsCareerUserId: Long?,
    val email: String?,
    val status: UserStatus,
    val role: UserRole,
)

/**
 * 기업 회원 로그인 검증에만 사용한다. 인코딩된 비밀번호를 담으므로 조회 결과를 응답에 그대로 노출하지 않는다.
 */
data class UserCredential(
    val userId: Long,
    val encodedPassword: String,
    val status: UserStatus,
    val role: UserRole,
)

data class UserAppendCommand(
    val letsCareerUserId: Long,
    val joinedAt: LocalDateTime,
)

data class CompanyAccountAppendCommand(
    val email: String,
    val encodedPassword: String,
    val joinedAt: LocalDateTime,
)

data class UserProfileSyncCommand(
    val userId: Long,
    val name: String?,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val letsCareerUpdatedAt: LocalDateTime?,
    val syncedAt: LocalDateTime,
)

data class CompanyProfileAppendCommand(
    val userId: Long,
    val organizationName: String,
    val managerName: String,
)
