package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRecipientDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenQueryRepository
import org.springframework.stereotype.Component

@Component
class UserFcmTokenRecipientReader internal constructor(private val repository: UserFcmTokenQueryRepository) {
    /** 활성 회원 수와 그중 토큰이 없는 회원 수. roles를 주면 그 역할의 회원만 센다. */
    fun countActiveAudience(roles: Set<UserRole>? = null): Pair<Long, Long> = repository.countActiveAudience(roles)

    fun readUpperUserId(): Long = repository.readUpperUserId()

    /** userIds를 주면 지정 회원을, roles를 주면 그 역할의 활성 회원을, 둘 다 없으면 전체 활성 회원을 읽는다. */
    fun readPage(afterUserId: Long, throughUserId: Long, userIds: Set<Long>?, limit: Int, roles: Set<UserRole>? = null): List<UserFcmTokenRecipientDto> {
        require(limit in 1..500)
        require(userIds == null || userIds.isNotEmpty())
        require(userIds == null || roles == null) { "지정 회원 발송에는 역할 조건을 함께 쓰지 않습니다." }
        return repository.readRecipients(afterUserId, throughUserId, userIds, roles, limit)
    }
}
