package com.ogonggo.userapi.notification.fcm.implement

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "ogonggo.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseApp(properties: FirebaseProperties): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }

        val credentials = properties.credentialsPath
            ?.takeIf(String::isNotBlank)
            ?.let { path -> FileInputStream(path).use(GoogleCredentials::fromStream) }
            ?: GoogleCredentials.getApplicationDefault()

        val optionsBuilder = FirebaseOptions.builder().setCredentials(credentials)
        properties.projectId?.takeIf(String::isNotBlank)?.let(optionsBuilder::setProjectId)
        return FirebaseApp.initializeApp(optionsBuilder.build())
    }

    @Bean
    @ConditionalOnBean(FirebaseApp::class)
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)
}
