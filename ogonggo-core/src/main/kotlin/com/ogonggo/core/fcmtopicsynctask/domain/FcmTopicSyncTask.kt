package com.ogonggo.core.fcmtopicsynctask.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/** 토큰별 최신 구독 희망 상태. 완료된 작업의 토큰 원문은 지운다. */
@Entity
@Table(name = "fcm_topic_sync_tasks", uniqueConstraints = [
    UniqueConstraint(name = "uk_fcm_topic_sync_token", columnNames = ["topic", "token_hash"]),
], indexes = [Index(name = "idx_fcm_topic_sync_due", columnList = "pending, next_attempt_at, id")])
internal class FcmTopicSyncTask(
    @Column(nullable = false, length = 200) val topic: String,
    @Column(name = "token_hash", nullable = false, length = 64) val tokenHash: String,
    @Column(length = 4096) var token: String?,
    @Column(name = "desired_subscribed", nullable = false) var desiredSubscribed: Boolean,
    @Column(name = "next_attempt_at", nullable = false) var nextAttemptAt: LocalDateTime,
) : BaseTimeEntity() {
    init {
        require(topic.isNotBlank() && topic.length <= 200)
        require(tokenHash.length == 64)
        require(!token.isNullOrBlank() && token!!.length <= 4096)
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
        protected set
    @Version var revision: Long = 0
        protected set
    @Column(nullable = false) var pending: Boolean = true
        protected set
    @Column(name = "attempt_count", nullable = false) var attemptCount: Int = 0
        protected set
    @Column(name = "last_error_code", length = 100) var lastErrorCode: String? = null
        protected set

    fun request(token: String, subscribed: Boolean, now: LocalDateTime) {
        if (desiredSubscribed == subscribed) return
        this.token = token
        desiredSubscribed = subscribed
        pending = true
        attemptCount = 0
        lastErrorCode = null
        nextAttemptAt = now
    }
}
