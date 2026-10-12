package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

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
            .apply { credentials(aws)?.let(::credentialsProvider) }
            .build()

    /** 클라이언트가 S3에 직접 올릴 presigned URL을 만든다. [s3Client]와 같은 인증을 쓴다. */
    @Bean
    fun s3Presigner(aws: CloudAwsProperties, properties: S3Properties): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(properties.region))
            .apply { credentials(aws)?.let(::credentialsProvider) }
            .build()

    /** 접근 키가 없으면 null을 돌려 AWS 기본 인증(환경변수, ECS 태스크 역할 등)을 쓰게 한다. */
    private fun credentials(aws: CloudAwsProperties): StaticCredentialsProvider? =
        if (aws.credentials.configured) {
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(aws.credentials.accessKey, aws.credentials.secretKey),
            )
        } else {
            null
        }
}
