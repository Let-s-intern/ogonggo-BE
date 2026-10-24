package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRecipientDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenQueryRepository
import org.springframework.stereotype.Component

@Component
class UserFcmTokenRecipientReader internal constructor(private val repository: UserFcmTokenQueryRepository) {
    fun countActiveAudience(): Pair<Long, Long> = repository.countActiveAudience()

    fun readUpperUserId(): Long = repository.readUpperUserId()

    fun readPage(afterUserId: Long, throughUserId: Long, userIds: Set<Long>?, limit: Int): List<UserFcmTokenRecipientDto> {
        require(limit in 1..500)
        require(userIds == null || userIds.isNotEmpty())
        return repository.readRecipients(afterUserId, throughUserId, userIds, limit)
    }
}
