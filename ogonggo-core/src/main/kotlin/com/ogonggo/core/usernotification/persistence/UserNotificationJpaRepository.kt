package com.ogonggo.core.usernotification.persistence

import com.ogonggo.core.usernotification.domain.UserNotification
import com.ogonggo.core.usernotification.domain.UserNotificationType
import org.springframework.data.jpa.repository.JpaRepository

internal interface UserNotificationJpaRepository : JpaRepository<UserNotification, Long> {
    fun existsByTypeAndCommentIdAndRecipientUserId(type: UserNotificationType, commentId: Long, recipientUserId: Long): Boolean
}
