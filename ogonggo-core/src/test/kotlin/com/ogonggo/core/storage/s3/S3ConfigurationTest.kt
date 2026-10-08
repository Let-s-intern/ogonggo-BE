package com.ogonggo.core.storage.s3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

class S3ConfigurationTest {

    private val contextRunner = ApplicationContextRunner().withUserConfiguration(S3Configuration::class.java)

    @Test
    fun `운영 설정의 cloud aws 항목으로 버킷과 리전과 공개 주소를 읽는다`() {
        contextRunner
            .withPropertyValues(
                "cloud.aws.s3.bucket=ogonggo-images",
                "cloud.aws.s3.public-base-url=https://cdn.ogonggo.test",
                "cloud.aws.credentials.access-key=test-access",
                "cloud.aws.credentials.secret-key=test-secret",
                "cloud.aws.region.static=us-east-1",
            )
            .run { context ->
                assertEquals(
                    S3Properties(
                        bucket = "ogonggo-images",
                        region = "us-east-1",
                        publicBaseUrl = "https://cdn.ogonggo.test",
                    ),
                    context.getBean(S3Properties::class.java),
                )
                assertEquals(Region.US_EAST_1, context.getBean(S3Client::class.java).serviceClientConfiguration().region())
            }
    }

    @Test
    fun `설정이 없어도 기동하고 버킷은 비어 있다`() {
        contextRunner.run { context ->
            assertEquals("", context.getBean(S3Properties::class.java).bucket)
        }
    }
}
