package com.ogonggo.core.notificationbatch.domain

import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class NotificationBatchTargetType(override val code: Int, override val desc: String) : EnumField {
    USER_IDS(1, "지정 회원"),
    ALL(2, "전체 활성 회원"),
}

/** 요청자·대상·본문·적재 건수를 남기는 운영 기록. 기기별 결과는 Notification이 소유한다. */
@Entity
@Table(name = "notification_batches")
internal class NotificationBatch(
    @Id @Column(length = 36)
    val id: String,
    @Column(name = "requested_by_user_id", nullable = false)
    val requestedByUserId: Long,
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 20)
    val targetType: NotificationBatchTargetType,
    @Column(name = "requested_user_ids_json", columnDefinition = "text")
    val requestedUserIdsJson: String?,
    @Column(nullable = false, length = 100)
    val title: String,
    @Column(nullable = false, length = 1000)
    val body: String,
    @Column(name = "data_json", nullable = false, columnDefinition = "text")
    val dataJson: String,
    @Column(name = "target_user_count", nullable = false)
    val targetUserCount: Long,
    @Column(name = "recipient_user_count", nullable = false)
    val recipientUserCount: Long,
    @Column(name = "enqueued_count", nullable = false)
    val enqueuedCount: Long,
    @Column(name = "skipped_no_token_user_count", nullable = false)
    val skippedNoTokenUserCount: Long,
    @Column(name = "skipped_inactive_user_count", nullable = false)
    val skippedInactiveUserCount: Long,
    @Column(name = "dispatch_expires_at", nullable = false)
    val dispatchExpiresAt: LocalDateTime,
) : BaseTimeEntity() {
    init {
        require(requestedByUserId > 0)
        require(title.isNotBlank() && title.length <= 100)
        require(body.isNotBlank() && body.length <= 1000)
        require(listOf(targetUserCount, recipientUserCount, enqueuedCount,
            skippedNoTokenUserCount, skippedInactiveUserCount).all { it >= 0 })
        require(targetUserCount == recipientUserCount + skippedNoTokenUserCount + skippedInactiveUserCount)
        require(enqueuedCount >= recipientUserCount)
    }
}
