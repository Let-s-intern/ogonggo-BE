package com.ogonggo.core.notificationbatch.persistence

import com.ogonggo.core.notificationbatch.domain.NotificationBroadcastRead
import com.ogonggo.core.user.domain.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface NotificationBroadcastReadJpaRepository : JpaRepository<NotificationBroadcastRead, Long> {
    fun existsByUserIdAndBatchId(userId: Long, batchId: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    fun lockUser(@Param("userId") userId: Long): User?
}
