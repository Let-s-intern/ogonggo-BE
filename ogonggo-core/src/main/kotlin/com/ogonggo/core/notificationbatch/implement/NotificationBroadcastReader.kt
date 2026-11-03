package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.notificationbatch.persistence.NotificationBroadcastQueryRepository
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/** 토픽으로 보낸 공통 공지를 회원 역할에 맞는 대상(전체·일반회원·기업회원)만 읽는다. */
@Component
class NotificationBroadcastReader internal constructor(private val queries: NotificationBroadcastQueryRepository) {
    fun readPage(userId: Long, role: UserRole, since: LocalDateTime, now: LocalDateTime, cursor: UserNotificationCursorDto?, limit: Int) =
        queries.page(userId, role, since, now, cursor, limit.also { require(it in 1..101) })
    fun countUnread(userId: Long, role: UserRole, since: LocalDateTime, now: LocalDateTime) = queries.countUnread(userId, role, since, now)
}
