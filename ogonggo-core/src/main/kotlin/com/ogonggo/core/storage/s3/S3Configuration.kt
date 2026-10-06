package com.ogonggo.core.storage.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudAwsProperties::class, LegacyS3Properties::class)
class S3Configuration {

    /** `cloud.aws` 값을 쓰고, 비어 있는 값만 이전 이름(`ogonggo.storage.s3`)에서 채운다. */
    @Bean
    fun s3Properties(
        aws: CloudAwsProperties,
        legacy: LegacyS3Properties,
    ): S3Properties = S3Properties(
        bucket = aws.s3.bucket.ifBlank { legacy.bucket },
        region = aws.region.static.ifBlank { legacy.region }.ifBlank { DEFAULT_REGION },
        publicBaseUrl = aws.s3.publicBaseUrl.ifBlank { legacy.publicBaseUrl },
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
