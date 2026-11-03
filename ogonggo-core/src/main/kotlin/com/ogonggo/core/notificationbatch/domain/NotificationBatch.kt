package com.ogonggo.core.notificationbatch.domain

import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.jpa.BaseTimeEntity
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class NotificationBatchTargetType(override val code: Int, override val desc: String) : EnumField {
    USER_IDS(1, "지정 회원"),
    ALL(2, "전체 활성 회원"),
    GENERAL_MEMBERS(3, "활성 일반 회원"),
    COMPANY_MEMBERS(4, "활성 기업 회원"),
    JOB_ROLES(5, "희망 직무가 맞는 활성 일반 회원"),
    ;

    /**
     * 회원 종류 기준 대상(전체·일반·기업)에 이 회원이 들어가는지.
     * 지정 회원·희망 직무 발송은 회원 종류만으로 정하지 않고 토픽도 쓰지 않으므로 false다.
     */
    fun includes(role: UserRole, status: UserStatus): Boolean = status == UserStatus.ACTIVE && when (this) {
        ALL -> true
        GENERAL_MEMBERS -> role in UserRole.GENERAL_MEMBER_ROLES
        COMPANY_MEMBERS -> role == UserRole.COMPANY
        USER_IDS, JOB_ROLES -> false
    }

    companion object {
        /** 토픽으로 보낼 수 있는 대상 종류. 회원 종류만으로 대상이 정해지는 것들이다. */
        val TOPIC_TARGETS: Set<NotificationBatchTargetType> = setOf(ALL, GENERAL_MEMBERS, COMPANY_MEMBERS)

        /** 이 역할의 활성 회원이 알림함에서 볼 수 있는 토픽 발송의 대상 종류. */
        fun visibleTopicTargets(role: UserRole): Set<NotificationBatchTargetType> =
            TOPIC_TARGETS.filterTo(mutableSetOf()) { it.includes(role, UserStatus.ACTIVE) }
    }
}

/** 요청자·대상·본문·적재 건수를 남기는 운영 기록. 기기별 결과는 Notification이 소유한다. */
@Entity
@Table(name = "notification_batches", indexes = [Index(name = "idx_notification_broadcast_created", columnList = "created_at, id")])
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
    @Column(name = "topic_name", length = 200)
    val topicName: String? = null,
    @Column(name = "url", length = 2048)
    val url: String? = null,
    /** 희망 직무 발송에서 관리자가 고른 렛츠커리어 직무 선택지(JSON 배열). 다른 대상은 null이다. */
    @Column(name = "target_job_roles_json", columnDefinition = "text")
    val targetJobRolesJson: String? = null,
    /** 광고성 발송이면 true. 렛츠커리어 마케팅 수신 동의자에게만 기기마다 보낸다. */
    @Column(name = "advertisement", nullable = false)
    val advertisement: Boolean = false,
    /** 광고성 발송에서 마케팅 수신에 동의하지 않아 뺀 활성 회원 수. */
    @Column(name = "skipped_no_marketing_consent_user_count", nullable = false)
    val skippedNoMarketingConsentUserCount: Long = 0,
) : BaseTimeEntity() {
    val deliveryTargetType: NotificationRecipientType
        get() = if (topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC

    init {
        require(requestedByUserId > 0)
        require(title.isNotBlank() && title.length <= 100)
        require(body.isNotBlank() && body.length <= 1000)
        require(listOf(targetUserCount, recipientUserCount, enqueuedCount,
            skippedNoTokenUserCount, skippedInactiveUserCount, skippedNoMarketingConsentUserCount).all { it >= 0 })
        require(targetUserCount == recipientUserCount + skippedNoTokenUserCount + skippedInactiveUserCount + skippedNoMarketingConsentUserCount)
        require(advertisement || skippedNoMarketingConsentUserCount == 0L)
        require((targetType == NotificationBatchTargetType.JOB_ROLES) == (targetJobRolesJson != null))
        if (topicName == null) require(enqueuedCount >= recipientUserCount)
        else {
            require(targetType in NotificationBatchTargetType.TOPIC_TARGETS)
            require(topicName.isNotBlank() && !advertisement)
            require(enqueuedCount == 1L && !url.isNullOrBlank())
        }
    }
}
