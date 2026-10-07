package com.ogonggo.userapi.notification.fcm.implement

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ogonggo.firebase")
data class FirebaseProperties(
    val enabled: Boolean = false,
    val credentialsPath: String? = null,
    val projectId: String? = null,
)
