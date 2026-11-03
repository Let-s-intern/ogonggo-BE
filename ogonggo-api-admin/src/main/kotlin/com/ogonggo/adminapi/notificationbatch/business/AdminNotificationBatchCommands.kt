package com.ogonggo.adminapi.notificationbatch.business

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType

data class CreateNotificationBatchCommand(
    val targetType: NotificationBatchTargetType,
    val userIds: List<Long>?,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    /** 희망 직무 발송에서 고른 렛츠커리어 직무 선택지. 다른 대상은 null이다. */
    val jobRoles: List<String>? = null,
    /** 광고성이면 렛츠커리어 마케팅 수신 동의자에게만 기기마다 보낸다. */
    val advertisement: Boolean = false,
)
