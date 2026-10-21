package com.ogonggo.adminapi.notificationbatch.business

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType

data class CreateNotificationBatchCommand(
    val targetType: NotificationBatchTargetType,
    val userIds: List<Long>?,
    val title: String,
    val body: String,
    val data: Map<String, String>,
)
