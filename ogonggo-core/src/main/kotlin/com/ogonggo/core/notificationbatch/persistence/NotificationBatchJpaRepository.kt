package com.ogonggo.core.notificationbatch.persistence

import com.ogonggo.core.notificationbatch.domain.NotificationBatch
import org.springframework.data.jpa.repository.JpaRepository

internal interface NotificationBatchJpaRepository : JpaRepository<NotificationBatch, String>
