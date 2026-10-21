package com.ogonggo.core.userfcmtoken.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "user_fcm_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_fcm_token_hash", columnNames = ["token_hash"]),
        UniqueConstraint(name = "uk_user_fcm_token_slot", columnNames = ["active_slot"]),
    ],
    indexes = [Index(name = "idx_user_fcm_token_user", columnList = "user_id, deleted_at, updated_at, id")],
)
internal class UserFcmToken(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "device_id", length = 36)
    val deviceId: String?,

    token: String,
    activeSlot: String,
) : BaseTimeEntity() {
    init {
        require(userId > 0)
        require(token.isNotBlank() && token.length <= 4096)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "token", length = 4096)
    var token: String? = token
        protected set

    @Column(name = "token_hash", length = 64)
    var tokenHash: String? = hash(token)
        protected set

    /** 실제 설치는 계정과 무관한 UUID, 기존 클라이언트는 회원별 슬롯을 사용한다. */
    @Column(name = "active_slot", length = 64)
    var activeSlot: String? = activeSlot
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun replace(token: String, now: LocalDateTime) {
        check(deletedAt == null)
        require(token.isNotBlank() && token.length <= 4096)
        this.token = token
        tokenHash = hash(token)
        updatedAt = now
    }

    fun revoke(now: LocalDateTime) {
        token = null
        tokenHash = null
        activeSlot = null
        deletedAt = now
    }

    companion object {
        fun slot(userId: Long, deviceId: UUID?): String = deviceId?.toString() ?: "legacy:$userId"

        fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
