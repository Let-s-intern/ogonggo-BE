package com.ogonggo.core.notification.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import com.ogonggo.core.enumeration.EnumField
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

/** 알림을 전달할 논리 채널이다. enum 등록만으로 해당 채널의 provider 발송기가 준비되는 것은 아니다. */
enum class NotificationChannel(override val code: Int, override val desc: String) : EnumField {
    KAKAO(1, "카카오 알림톡"),
    EMAIL(2, "이메일"),
    FCM(3, "푸시 알림"),
}

/** provider에 접수되면 SENT다. 사용자 단말 도착 여부까지 의미하지 않는다. */
enum class NotificationStatus(override val code: Int, override val desc: String) : EnumField {
    PENDING(1, "발송 대기"),
    SENT(2, "provider 접수"),
    FAILED(3, "발송 실패"),
    UNKNOWN(4, "provider 접수 여부 미확정"),
    PROCESSING(5, "provider 처리 중"),
}

/** 알림 전송을 맡는 provider다. 채널별 처리는 현재 provider 연결을 기준으로 한다. */
enum class NotificationProvider(val channel: NotificationChannel) {
    NHN_ALIMTALK(NotificationChannel.KAKAO),
    EMAIL_SMTP(NotificationChannel.EMAIL),
    FIREBASE_FCM(NotificationChannel.FCM),
}

/** 수신자 한 명·채널 한 개에 대한 알림과 발송 결과를 보존한다. */
@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notification_deduplication_key", columnNames = ["deduplication_key"]),
    ],
    indexes = [
        Index(name = "idx_notification_user_notification", columnList = "user_notification_id"),
        Index(name = "idx_notification_batch", columnList = "batch_id, status"),
        Index(name = "idx_notification_due", columnList = "status, scheduled_at, id"),
        Index(name = "idx_notification_processing_stale", columnList = "status, channel, processing_started_at, id"),
        Index(name = "idx_notification_cleanup", columnList = "status, updated_at, id"),
    ],
)
internal class Notification(
    /** 같은 논리 알림 재수신을 막는 키. 새 발송 건으로 취급할 알림은 새 키를 받아야 한다. */
    @Column(name = "deduplication_key", nullable = false, length = 200)
    val deduplicationKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val channel: NotificationChannel,

    /** 카카오에서는 NHN 등록 코드, 다른 채널에서는 애플리케이션 템플릿 식별자다. */
    @Column(name = "template_code", nullable = false, length = 100)
    val templateCode: String,

    /** 내부 사용자와 연결할 수 없는 수신 대상도 허용하므로 nullable이다. */
    @Column(name = "recipient_user_id")
    val recipientUserId: Long? = null,

    @Column(name = "user_notification_id")
    val userNotificationId: Long? = null,

    recipientAddress: String,
    payloadJson: String,

    /** 이 시각이 되면 dispatcher가 provider 발송을 시도한다. */
    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,

    @Column(name = "batch_id", length = 36)
    val batchId: String? = null,

    /** 업무상 발송 기한. null이면 기한 제한 없이 대기하며, 관리자 일괄 발송은 24시간 기한을 지정한다. */
    @Column(name = "dispatch_expires_at")
    val dispatchExpiresAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    val recipientType: NotificationRecipientType = NotificationRecipientType.DIRECT,
) : BaseTimeEntity() {
    init {
        require(recipientType != NotificationRecipientType.TOPIC ||
            (channel == NotificationChannel.FCM && recipientUserId == null && userNotificationId == null))
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING
        protected set

    /** 외부 요청 시도마다 바뀌는 fencing token. stale UNKNOWN은 최신 시도의 늦은 결과를 기다린다. */
    @Column(name = "processing_attempt_id", length = 36)
    var processingAttemptId: String? = null
        protected set

    /** 현재 provider 요청을 시작하기 직전에 기록한 시각. provider별 stale timeout의 기준이다. */
    @Column(name = "processing_started_at")
    var processingStartedAt: LocalDateTime? = null
        protected set

    /** 첫 시도 시각은 재시도해도 유지해 NHN 멱등성 창을 연장하지 않는다. */
    @Column(name = "first_attempt_at")
    var firstAttemptAt: LocalDateTime? = null
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        protected set

    /** 주소와 payload는 30일 이내 발송 결과를 추적하는 동안 보존한다. */
    @Column(name = "recipient_address", length = 4096)
    var recipientAddress: String? = recipientAddress
        protected set

    @Column(name = "payload_json", columnDefinition = "text")
    var payloadJson: String? = payloadJson
        protected set

    /** provider 접수 응답에서 얻은 식별자. 사용자 단말 전달 확인용 ID는 아니다. */
    @Column(name = "provider_message_id", length = 255)
    var providerMessageId: String? = null
        protected set

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null
        protected set

    /** provider 응답 또는 내부 처리 실패를 운영자가 추적할 때 사용하는 코드다. */
    @Column(name = "result_code", length = 100)
    var resultCode: String? = null
        protected set

    /** 원본 resultCode의 운영상 의미를 채널 공통 분류로 보존한다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "result_category", length = 50)
    var resultCategory: NotificationFailureCategory? = null
        protected set

    /** provider가 제공한 민감정보 없는 오류 상세. 운영 추적을 위해 최대 500자만 보존한다. */
    @Column(name = "result_detail", length = 500)
    var resultDetail: String? = null
        protected set

    fun markSent(attemptId: String, now: LocalDateTime, providerMessageId: String?) {
        checkCurrentAttempt(attemptId)
        status = NotificationStatus.SENT
        sentAt = now
        this.providerMessageId = providerMessageId
        resultCode = null
        resultCategory = null
        resultDetail = null
        clearProcessing()
    }

    fun markFailed(attemptId: String, resultCode: String, failureCategory: NotificationFailureCategory, detail: String?) {
        checkCurrentAttempt(attemptId)
        status = NotificationStatus.FAILED
        this.resultCode = resultCode
        resultCategory = failureCategory
        resultDetail = detail?.trim()?.take(RESULT_DETAIL_MAX_LENGTH)
        clearProcessing()
    }

    fun markUnknown(attemptId: String, resultCode: String, failureCategory: NotificationFailureCategory, detail: String?) {
        checkCurrentAttempt(attemptId)
        status = NotificationStatus.UNKNOWN
        this.resultCode = resultCode
        resultCategory = failureCategory
        resultDetail = detail?.trim()?.take(RESULT_DETAIL_MAX_LENGTH)
        clearProcessing()
    }

    private fun checkCurrentAttempt(attemptId: String) {
        check(status == NotificationStatus.PROCESSING && processingAttemptId == attemptId)
    }

    private fun clearProcessing() {
        processingAttemptId = null
        processingStartedAt = null
    }

    private companion object {
        const val RESULT_DETAIL_MAX_LENGTH = 500
    }
}
