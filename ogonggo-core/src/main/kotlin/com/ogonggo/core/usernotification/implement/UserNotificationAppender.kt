package com.ogonggo.core.usernotification.implement

import com.ogonggo.core.usernotification.domain.UserNotification
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import com.ogonggo.core.usernotification.persistence.UserNotificationJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserNotificationAppender internal constructor(private val repository: UserNotificationJpaRepository) {
    /** 재처리면 null. 동시 insert 경합은 DB 제약으로 전체 업무 트랜잭션을 롤백한다. */
    @Transactional
    fun append(dto: UserNotificationAppendDto): Long? {
        val exists = if (dto.type == UserNotificationType.ADMIN_NOTIFICATION) {
            repository.existsByBatchIdAndRecipientUserId(checkNotNull(dto.batchId), dto.recipientUserId)
        } else {
            repository.existsByTypeAndCommentIdAndRecipientUserId(dto.type, checkNotNull(dto.commentId), dto.recipientUserId)
        }
        if (exists) return null
        val notification = repository.saveAndFlush(
            UserNotification(
                recipientUserId = dto.recipientUserId,
                type = dto.type,
                actorUserId = dto.actorUserId,
                postId = dto.postId,
                commentId = dto.commentId,
                actorNickname = dto.actorNickname,
                actorProfileImageUrl = dto.actorProfileImageUrl,
                postTitle = dto.postTitle,
                preview = dto.preview,
                pushSkipReason = dto.pushSkipReason,
                batchId = dto.batchId,
                url = dto.url,
            ),
        )
        return checkNotNull(notification.id)
    }
}
