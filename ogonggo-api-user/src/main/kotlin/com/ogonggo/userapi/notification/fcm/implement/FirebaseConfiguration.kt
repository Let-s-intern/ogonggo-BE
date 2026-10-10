package com.ogonggo.userapi.notification.fcm.implement

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "ogonggo.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseApp(properties: FirebaseProperties): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }

        val credentials = serviceAccountCredentials(properties)

        val optionsBuilder = FirebaseOptions.builder().setCredentials(credentials)
        properties.projectId?.takeIf(String::isNotBlank)?.let(optionsBuilder::setProjectId)
        return FirebaseApp.initializeApp(optionsBuilder.build())
    }

    @Bean
    @ConditionalOnBean(FirebaseApp::class)
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)

    private fun serviceAccountCredentials(properties: FirebaseProperties): GoogleCredentials {
        val projectId = properties.projectId?.trim()
        val clientId = properties.clientId?.trim()
        val clientEmail = properties.clientEmail?.trim()
        val privateKeyId = properties.privateKeyId?.trim()
        val privateKey = properties.privateKey
            ?.replace("\\n", "\n")
            ?.trim()
        val configuredValues = listOf(projectId, clientId, clientEmail, privateKeyId, privateKey)
        val configuredCount = configuredValues.count { !it.isNullOrBlank() }

        return when (configuredCount) {
            0 -> GoogleCredentials.getApplicationDefault()
            5 -> ServiceAccountCredentials.fromPkcs8(
                requireNotNull(clientId),
                requireNotNull(clientEmail),
                requireNotNull(privateKey),
                requireNotNull(privateKeyId),
                emptyList(),
            )
            else -> error("ogonggo.firebase의 서비스 계정 프로퍼티를 모두 설정해야 합니다.")
        }
    }
}
