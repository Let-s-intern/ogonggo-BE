package com.ogonggo.core.notificationbatch.persistence

import com.ogonggo.core.notificationbatch.domain.QNotificationBatch.notificationBatch as batch
import com.ogonggo.core.notificationbatch.domain.QNotificationBroadcastRead.notificationBroadcastRead as receipt
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import com.ogonggo.core.usernotification.implement.dto.UserNotificationDto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class NotificationBroadcastQueryRepository(private val queries: JPAQueryFactory) {
    private fun visible(since: LocalDateTime, now: LocalDateTime) = batch.topicName.isNotNull
        .and(batch.createdAt.goe(since)).and(batch.createdAt.loe(now))

    fun page(userId: Long, since: LocalDateTime, now: LocalDateTime, cursor: UserNotificationCursorDto?, limit: Int): List<UserNotificationDto> = queries
        .select(batch, receipt.readAt).from(batch)
        .leftJoin(receipt).on(receipt.batchId.eq(batch.id), receipt.userId.eq(userId))
        .where(visible(since, now), cursor?.let {
            batch.createdAt.lt(it.createdAt).or(batch.createdAt.eq(it.createdAt).and(
                if (it.broadcastId != null) batch.id.lt(it.broadcastId) else batch.id.isNull))
        }).orderBy(batch.createdAt.desc(), batch.id.desc()).limit(limit.toLong()).fetch().map { row ->
            val item = checkNotNull(row.get(batch))
            val preview = item.body.substring(0, item.body.offsetByCodePoints(0, minOf(100, item.body.codePointCount(0, item.body.length))))
            UserNotificationDto(id = null, type = UserNotificationType.ADMIN_NOTIFICATION,
                actorNickname = "오공고", actorProfileImageUrl = null, postTitle = item.title, preview = preview,
                postId = null, commentId = null, readAt = row.get(receipt.readAt), createdAt = item.createdAt,
                url = item.url, broadcastId = item.id, body = item.body)
        }

    fun countUnread(userId: Long, since: LocalDateTime, now: LocalDateTime): Long = queries
        .select(batch.count()).from(batch).leftJoin(receipt).on(receipt.batchId.eq(batch.id), receipt.userId.eq(userId))
        .where(visible(since, now), receipt.id.isNull).fetchOne() ?: 0

    fun isVisible(batchId: String, since: LocalDateTime, now: LocalDateTime): Boolean = queries
        .selectOne().from(batch).where(visible(since, now), batch.id.eq(batchId)).fetchFirst() != null

    fun unreadIds(userId: Long, since: LocalDateTime, now: LocalDateTime): List<String> = queries
        .select(batch.id).from(batch).leftJoin(receipt).on(receipt.batchId.eq(batch.id), receipt.userId.eq(userId))
        .where(visible(since, now), receipt.id.isNull).orderBy(batch.id.asc()).fetch()
}
