package com.ogonggo.core.notification.domain

import com.ogonggo.core.common.BaseTimeEntity
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
}

/** 수신자 한 명·채널 한 개에 대한 알림과 발송 결과를 보존한다. */
@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notification_deduplication_key", columnNames = ["deduplication_key"]),
    ],
    indexes = [
        Index(name = "idx_notification_due", columnList = "status, scheduled_at, id"),
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

    recipientAddress: String,
    payloadJson: String,

    /** 이 시각이 되면 dispatcher가 provider 발송을 시도한다. */
    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING
        protected set

    /** 주소와 payload는 30일 이내 발송 결과를 추적하는 동안 보존한다. */
    @Column(name = "recipient_address", length = 512)
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

    fun markSent(now: LocalDateTime, providerMessageId: String?) {
        check(status == NotificationStatus.PENDING)
        status = NotificationStatus.SENT
        sentAt = now
        this.providerMessageId = providerMessageId
        resultCode = null
    }

    fun markFailed(resultCode: String) {
        check(status == NotificationStatus.PENDING)
        status = NotificationStatus.FAILED
        this.resultCode = resultCode
    }
}
