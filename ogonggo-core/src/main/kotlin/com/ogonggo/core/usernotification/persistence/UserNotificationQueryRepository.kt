package com.ogonggo.core.usernotification.persistence

import com.ogonggo.core.usernotification.domain.QUserNotification.userNotification as notification
import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class UserNotificationQueryRepository(private val queryFactory: JPAQueryFactory) {
    fun findPage(userId: Long, since: LocalDateTime, cursor: UserNotificationCursorDto?, limit: Int) =
        queryFactory.selectFrom(notification)
            .where(
                visible(userId, since),
                cursor?.let {
                    notification.createdAt.lt(it.createdAt).or(
                        notification.createdAt.eq(it.createdAt).and(
                            if (it.broadcastId != null) notification.id.isNotNull else notification.id.lt(checkNotNull(it.id))),
                    )
                },
            )
            .orderBy(notification.createdAt.desc(), notification.id.desc())
            .limit(limit.toLong())
            .fetch()

    fun countUnread(userId: Long, since: LocalDateTime): Long = queryFactory
        .select(notification.count()).from(notification)
        .where(visible(userId, since), notification.readAt.isNull)
        .fetchOne() ?: 0

    fun existsVisible(userId: Long, id: Long, since: LocalDateTime): Boolean = queryFactory
        .selectOne().from(notification)
        .where(visible(userId, since), notification.id.eq(id))
        .fetchFirst() != null

    fun markRead(userId: Long, id: Long, since: LocalDateTime, now: LocalDateTime): Long = queryFactory
        .update(notification)
        .set(notification.readAt, now).set(notification.updatedAt, now)
        .where(visible(userId, since), notification.id.eq(id), notification.readAt.isNull)
        .execute()

    /** 처리 시작 때 보이는 ID 상한을 고정한다. 뒤에 삽입된 알림은 같은 시각이어도 읽지 않는다. */
    fun findReadBoundary(userId: Long, since: LocalDateTime, now: LocalDateTime): Long? = queryFactory
        .select(notification.id.max()).from(notification)
        .where(visible(userId, since), notification.createdAt.loe(now))
        .fetchOne()

    fun markAllRead(userId: Long, throughId: Long, since: LocalDateTime, now: LocalDateTime): Long = queryFactory
        .update(notification)
        .set(notification.readAt, now).set(notification.updatedAt, now)
        .where(
            visible(userId, since), notification.id.loe(throughId),
            notification.createdAt.loe(now), notification.readAt.isNull,
        )
        .execute()

    private fun visible(userId: Long, since: LocalDateTime) = notification.recipientUserId.eq(userId)
        .and(notification.deletedAt.isNull).and(notification.createdAt.goe(since))
}
