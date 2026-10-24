package com.ogonggo.core.notificationbatch.persistence

import com.ogonggo.core.notification.domain.QNotification.notification
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchStatusCountDto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
internal class NotificationBatchQueryRepository(private val queryFactory: JPAQueryFactory) {
    /** 한 번의 집계로 상태·코드별 건수를 읽는다. */
    fun countResults(batchId: String): List<NotificationBatchStatusCountDto> = queryFactory
        .select(notification.status, notification.resultCode, notification.count())
        .from(notification).where(notification.batchId.eq(batchId))
        .groupBy(notification.status, notification.resultCode)
        .orderBy(notification.status.asc(), notification.resultCode.asc()).fetch()
        .map { NotificationBatchStatusCountDto(
            checkNotNull(it.get(notification.status)), it.get(notification.resultCode),
            checkNotNull(it.get(notification.count())),
        ) }
}
