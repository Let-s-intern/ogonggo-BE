package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.ec2instance.implement.Ec2InstanceProperties
import com.ogonggo.core.storage.s3.CloudAwsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Ec2InstanceProperties::class, CloudAwsProperties::class)
class AdminEc2InstanceConfiguration {

    /**
     * 지역은 S3와 같은 `cloud.aws` 지역을 쓰고, 접근 키는 인스턴스가 있는 계정의 `ogonggo.ec2-instance` 키를 쓴다.
     * 리대시·크롤러는 S3 버킷과 다른 AWS 계정에 있어서 `cloud.aws` 접근 키로는 인스턴스를 찾지 못한다.
     * 접근 키가 없으면 AWS 기본 인증(환경변수, ECS 태스크 역할 등)을 쓴다.
     */
    @Bean
    fun ec2Client(aws: CloudAwsProperties, properties: Ec2InstanceProperties): Ec2Client =
        Ec2Client.builder()
            .region(Region.of(aws.region.static))
            .apply {
                if (properties.credentialsConfigured) {
                    credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(properties.accessKey, properties.secretKey),
                        ),
                    )
                }
            }
            .build()
}
