package com.ogonggo.core.userfcmtoken.persistence

import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.userfcmtoken.domain.QUserFcmToken.userFcmToken as token
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenDto
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRecipientDto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
internal class UserFcmTokenQueryRepository(private val queryFactory: JPAQueryFactory) {
    /** 활성 회원 수와 그중 토큰이 없는 회원 수. roles를 주면 그 역할의 회원만 센다. */
    fun countActiveAudience(roles: Set<UserRole>?): Pair<Long, Long> {
        val noTokenId = com.querydsl.core.types.dsl.CaseBuilder().`when`(token.id.isNull)
            .then(user.id).otherwise(com.querydsl.core.types.dsl.Expressions.nullExpression<Long>())
        val row = queryFactory.select(user.id.countDistinct(), noTokenId.countDistinct()).from(user)
            .leftJoin(token).on(token.userId.eq(user.id), token.deletedAt.isNull, token.token.isNotNull)
            .where(user.status.eq(UserStatus.ACTIVE), roles?.let { user.role.`in`(it) }).fetchOne()!!
        return checkNotNull(row.get(user.id.countDistinct())) to checkNotNull(row.get(noTokenId.countDistinct()))
    }

    fun readUpperUserId(): Long = queryFactory.select(user.id.max()).from(user).fetchOne() ?: 0

    /** userIds를 주면 그 회원을 상태와 무관하게 읽고, 없으면 활성 회원만 읽는다. roles를 주면 그 역할의 활성 회원만 읽는다. */
    fun readRecipients(afterUserId: Long, throughUserId: Long, userIds: Set<Long>?, roles: Set<UserRole>?, limit: Int): List<UserFcmTokenRecipientDto> {
        val users = queryFactory.select(user.id, user.status).from(user)
            .where(user.id.gt(afterUserId), user.id.loe(throughUserId),
                userIds?.let { user.id.`in`(it) } ?: user.status.eq(UserStatus.ACTIVE),
                roles?.let { user.role.`in`(it) })
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
