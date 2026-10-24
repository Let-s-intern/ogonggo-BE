package com.ogonggo.core.notificationbatch.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notification_broadcast_reads", uniqueConstraints = [
    UniqueConstraint(name = "uk_broadcast_read_user_batch", columnNames = ["user_id", "batch_id"]),
])
internal class NotificationBroadcastRead(
    @Column(name = "user_id", nullable = false) val userId: Long,
    @Column(name = "batch_id", nullable = false, length = 36) val batchId: String,
    @Column(name = "read_at", nullable = false) val readAt: LocalDateTime,
) {
    init {
        require(userId > 0)
        require(batchId.isNotBlank() && batchId.length <= 36)
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
