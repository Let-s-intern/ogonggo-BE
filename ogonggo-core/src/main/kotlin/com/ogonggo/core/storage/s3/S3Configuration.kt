package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudAwsProperties::class)
class S3Configuration {

    /** 운영 설정의 `cloud.aws` 값으로 버킷과 표시용 URL 설정을 만든다. */
    @Bean
    fun s3Properties(aws: CloudAwsProperties): S3Properties = S3Properties(
        bucket = aws.s3.bucket,
        region = aws.region.static.ifBlank { DEFAULT_REGION },
        publicBaseUrl = aws.s3.publicBaseUrl,
    )

    @Bean
    fun s3Client(aws: CloudAwsProperties, properties: S3Properties): S3Client =
        S3Client.builder()
            .region(Region.of(properties.region))
            .apply {
                if (aws.credentials.configured) {
                    credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(aws.credentials.accessKey, aws.credentials.secretKey),
                        ),
                    )
                }
            }
            .build()
}
