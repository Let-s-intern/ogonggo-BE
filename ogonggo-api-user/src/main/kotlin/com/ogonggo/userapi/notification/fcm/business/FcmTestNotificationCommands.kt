package com.ogonggo.userapi.notification.fcm.business

data class SendFcmTestNotificationCommand(
    val title: String,
    val body: String,
    val data: Map<String, String>,
)
