package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(NotificationProcessingProperties::class)
class NotificationProcessingPolicy(private val properties: NotificationProcessingProperties) {
    fun settings(provider: NotificationProvider): NotificationProviderProcessingSettings =
        when (provider) {
            NotificationProvider.NHN_ALIMTALK -> properties.nhnAlimTalk
            NotificationProvider.EMAIL_SMTP -> properties.emailSmtp
            NotificationProvider.FIREBASE_FCM -> properties.firebaseFcm
        }
}
