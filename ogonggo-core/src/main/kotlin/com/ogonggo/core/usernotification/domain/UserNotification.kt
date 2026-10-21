package com.ogonggo.core.usernotification.domain

import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

enum class UserNotificationType(override val code: Int, override val desc: String) : EnumField {
    SIDE_STUDY_COMMENT_REPLY(1, "모집글 댓글의 새 답글"),
    CONCERN_COMMENT(2, "취준고민의 새 댓글"),
}

enum class UserNotificationPushSkipReason(override val code: Int, override val desc: String) : EnumField {
    NO_ACTIVE_TOKEN(1, "활성 토큰 없음"),
}

/** 회원에게 보여 주는 소식 한 건. 기기별 발송 결과와 읽음 상태는 독립적이다. */
@Entity
@Table(
    name = "user_notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_notification_event",
            columnNames = ["type", "comment_id", "recipient_user_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_user_notification_recipient_created_id", columnList = "recipient_user_id, created_at, id"),
    ],
)
internal class UserNotification(
    @Column(name = "recipient_user_id", nullable = false)
    val recipientUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: UserNotificationType,
    @Column(name = "actor_user_id", nullable = false)
    val actorUserId: Long,
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    @Column(name = "comment_id", nullable = false)
    val commentId: Long,
    @Column(name = "actor_nickname", nullable = false, length = 100)
    val actorNickname: String,
    @Column(name = "actor_profile_image_url", length = 2048)
    val actorProfileImageUrl: String?,
    @Column(name = "post_title", nullable = false, length = 255)
    val postTitle: String,
    @Column(nullable = false, length = 100)
    val preview: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "push_skip_reason", length = 30)
    val pushSkipReason: UserNotificationPushSkipReason?,
) : BaseTimeEntity() {
    init {
        require(recipientUserId > 0 && actorUserId > 0 && postId > 0 && commentId > 0)
        require(actorNickname.isNotBlank() && actorNickname.length <= 100)
        require(postTitle.isNotBlank() && postTitle.length <= 255)
        require(preview.codePointCount(0, preview.length) <= 100)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) deletedAt = now
    }
}
