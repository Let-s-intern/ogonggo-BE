package com.ogonggo.core.notificationbatch.persistence

import com.ogonggo.core.notification.domain.QNotification.notification
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notificationbatch.domain.NotificationBatch
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.domain.QNotificationBatch.notificationBatch as batch
import com.ogonggo.core.notificationbatch.domain.QNotificationBroadcastRead.notificationBroadcastRead as receipt
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.usernotification.domain.QUserNotification.userNotification as inbox
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class NotificationBatchStatisticsQueryRepository(private val queries: JPAQueryFactory) {
    private fun period(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?) =
        batch.createdAt.goe(from).and(batch.createdAt.lt(to))
            .and(targetType?.let { batch.targetType.eq(it) })

    fun batches(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?): List<NotificationBatch> =
        queries.selectFrom(batch).where(period(from, to, targetType)).orderBy(batch.createdAt.asc(), batch.id.asc()).fetch()

    fun statusCounts(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?): Map<String, Map<NotificationStatus, Long>> =
        queries.select(notification.batchId, notification.status, notification.count())
            .from(notification).join(batch).on(batch.id.eq(notification.batchId))
            .where(period(from, to, targetType))
            .groupBy(notification.batchId, notification.status).fetch()
            .groupBy { checkNotNull(it.get(notification.batchId)) }
            .mapValues { (_, rows) -> rows.associate { checkNotNull(it.get(notification.status)) to checkNotNull(it.get(notification.count())) } }

    fun directEligibleCounts(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?): Map<String, Long> =
        queries.select(inbox.batchId, inbox.count()).from(inbox).join(batch).on(batch.id.eq(inbox.batchId))
            .where(period(from, to, targetType), batch.topicName.isNull)
            .groupBy(inbox.batchId).fetch()
            .associate { checkNotNull(it.get(inbox.batchId)) to checkNotNull(it.get(inbox.count())) }

    fun directReadCounts(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?): Map<String, Long> =
        queries.select(inbox.batchId, inbox.count()).from(inbox).join(batch).on(batch.id.eq(inbox.batchId))
            .where(period(from, to, targetType), batch.topicName.isNull, inbox.readAt.isNotNull)
            .groupBy(inbox.batchId).fetch()
            .associate { checkNotNull(it.get(inbox.batchId)) to checkNotNull(it.get(inbox.count())) }

    /** 공통 알림 읽음은 현재 활성 회원과 현재 역할로 노출되는 기록만 센다. */
    fun topicReadCounts(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?): Map<String, Long> =
        queries.select(receipt.batchId, receipt.count()).from(receipt)
            .join(batch).on(batch.id.eq(receipt.batchId))
            .join(user).on(user.id.eq(receipt.userId))
            .where(period(from, to, targetType), batch.topicName.isNotNull, user.status.eq(UserStatus.ACTIVE),
                batch.targetType.eq(NotificationBatchTargetType.ALL)
                    .or(batch.targetType.eq(NotificationBatchTargetType.GENERAL_MEMBERS).and(user.role.`in`(UserRole.GENERAL_MEMBER_ROLES)))
                    .or(batch.targetType.eq(NotificationBatchTargetType.COMPANY_MEMBERS).and(user.role.eq(UserRole.COMPANY))))
            .groupBy(receipt.batchId).fetch()
            .associate { checkNotNull(it.get(receipt.batchId)) to checkNotNull(it.get(receipt.count())) }
}
