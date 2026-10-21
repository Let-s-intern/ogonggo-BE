package com.ogonggo.core.userfcmtoken.persistence

import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.userfcmtoken.domain.QUserFcmToken.userFcmToken as token
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenDto
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRecipientDto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
internal class UserFcmTokenQueryRepository(private val queryFactory: JPAQueryFactory) {
    fun readUpperUserId(): Long = queryFactory.select(user.id.max()).from(user).fetchOne() ?: 0

    fun readRecipients(afterUserId: Long, throughUserId: Long, userIds: Set<Long>?, limit: Int): List<UserFcmTokenRecipientDto> {
        val users = queryFactory.select(user.id, user.status).from(user)
            .where(user.id.gt(afterUserId), user.id.loe(throughUserId),
                userIds?.let { user.id.`in`(it) } ?: user.status.eq(UserStatus.ACTIVE))
            .orderBy(user.id.asc()).limit(limit.toLong()).fetch()
        if (users.isEmpty()) return emptyList()
        val activeIds = users.filter { it.get(user.status) == UserStatus.ACTIVE }.map { checkNotNull(it.get(user.id)) }
        val tokens = if (activeIds.isEmpty()) emptyMap() else queryFactory
            .select(token.userId, token.id, token.token).from(token)
            .where(token.userId.`in`(activeIds), token.deletedAt.isNull)
            .orderBy(token.id.asc()).fetch()
            .groupBy({ checkNotNull(it.get(token.userId)) }, {
                UserFcmTokenDto(checkNotNull(it.get(token.id)), checkNotNull(it.get(token.token)))
            })
        return users.map {
            val id = checkNotNull(it.get(user.id))
            UserFcmTokenRecipientDto(id, checkNotNull(it.get(user.status)), tokens[id].orEmpty())
        }
    }
}
