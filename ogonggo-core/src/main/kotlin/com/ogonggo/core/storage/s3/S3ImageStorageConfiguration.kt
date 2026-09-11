package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(S3ImageStorageProperties::class)
class S3ImageStorageConfiguration {

    @Bean
    fun s3Client(properties: S3ImageStorageProperties): S3Client =
        S3Client.builder()
            .region(Region.of(properties.region))
            .build()
}
