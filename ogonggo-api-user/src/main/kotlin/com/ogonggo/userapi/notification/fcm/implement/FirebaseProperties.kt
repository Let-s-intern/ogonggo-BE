package com.ogonggo.userapi.notification.fcm.implement

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ogonggo.firebase")
data class FirebaseProperties(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val clientId: String? = null,
    val clientEmail: String? = null,
    val privateKeyId: String? = null,
    val privateKey: String? = null,
)
