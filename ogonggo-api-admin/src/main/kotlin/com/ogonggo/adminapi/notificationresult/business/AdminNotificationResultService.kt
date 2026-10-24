package com.ogonggo.adminapi.notificationresult.business

import com.ogonggo.core.notification.delivery.implement.NotificationResultReader
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryRecordDto
import org.springframework.stereotype.Service

@Service
class AdminNotificationResultService(
    private val notificationResultReader: NotificationResultReader,
) {
    fun getRecent(): List<NotificationDeliveryRecordDto> = notificationResultReader.readRecent(RECENT_LIMIT)

    private companion object {
        const val RECENT_LIMIT = 100
    }
}
