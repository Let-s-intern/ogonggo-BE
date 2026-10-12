package com.ogonggo.core.storage.s3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.model.CopyObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import java.time.Duration

class S3ObjectClientTest {

    @Test
    fun `S3에 저장한 이미지의 표시용 URL을 public base URL로 만든다`() {
        var putObjectCalled = false
        val s3Client = Mockito.mock(
            S3Client::class.java,
            Answer { invocation ->
                if (invocation.method.name == "putObject") {
                    putObjectCalled = true
                    PutObjectResponse.builder().build()
                } else {
                    Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            },
        )
        val storage = S3ObjectClient(
            s3Client = s3Client,
            s3Presigner = Mockito.mock(S3Presigner::class.java),
            properties = S3Properties(
                bucket = "ogonggo-images",
                region = "ap-northeast-2",
                publicBaseUrl = "https://cdn.example.com",
            ),
        )

        val url = storage.put(
            key = "images/image-id.png",
            content = byteArrayOf(1, 2, 3),
            contentType = "image/png",
        )

        assertEquals("https://cdn.example.com/images/image-id.png", url)
        assertTrue(putObjectCalled)
    }

    @Test
    fun `S3 bucket이 설정되지 않으면 저장하지 않는다`() {
        val s3Client = Mockito.mock(S3Client::class.java)
        val storage = S3ObjectClient(
            s3Client = s3Client,
            s3Presigner = Mockito.mock(S3Presigner::class.java),
            properties = S3Properties(),
        )

        assertFalse(storage.isConfigured())
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            storage.put("images/image-id.png", byteArrayOf(1), "image/png")
        }
        Mockito.verifyNoInteractions(s3Client)
    }

    @Test
    fun `S3에서 이미지 객체를 삭제한다`() {
        var deleteObjectCalled = false
        val s3Client = Mockito.mock(
            S3Client::class.java,
            Answer { invocation ->
                if (invocation.method.name == "deleteObject") {
                    deleteObjectCalled = true
                    DeleteObjectResponse.builder().build()
                } else {
                    Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            },
        )
        val storage = S3ObjectClient(
            s3Client = s3Client,
            s3Presigner = Mockito.mock(S3Presigner::class.java),
            properties = S3Properties(bucket = "ogonggo-images"),
        )

        storage.delete("images/image-id.png")

        assertTrue(deleteObjectCalled)
    }

    @Test
    fun `S3에서 이미지 객체를 새 키로 복사한다`() {
        var copyObjectCalled = false
        val s3Client = Mockito.mock(
            S3Client::class.java,
            Answer { invocation ->
                if (invocation.method.name == "copyObject") {
                    copyObjectCalled = true
                    CopyObjectResponse.builder().build()
                } else {
                    Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            },
        )
        val storage = S3ObjectClient(
            s3Client = s3Client,
            s3Presigner = Mockito.mock(S3Presigner::class.java),
            properties = S3Properties(bucket = "ogonggo-images"),
        )

        storage.copy("images/source.png", "images/target.png", "image/png")

        assertTrue(copyObjectCalled)
    }

    @Test
    fun `presigned PUT URL은 형식과 크기와 캐시 헤더를 서명에 묶는다`() {
        // given
        val presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
            .build()
        val storage = S3ObjectClient(
            s3Client = Mockito.mock(S3Client::class.java),
            s3Presigner = presigner,
            properties = S3Properties(bucket = "ogonggo-images", region = "ap-northeast-2"),
        )

        // when
        val upload = storage.presignPut("images/image-id.png", "image/png", 1_100_000L, Duration.ofMinutes(5))

        // then
        assertTrue(upload.url.startsWith("https://ogonggo-images.s3.ap-northeast-2.amazonaws.com/images/image-id.png?"))
        val signedHeaders = Regex("X-Amz-SignedHeaders=([^&]+)").find(upload.url)!!.groupValues[1]
        listOf("cache-control", "content-length", "content-type").forEach { assertTrue(signedHeaders.contains(it), it) }
        assertTrue(upload.url.contains("X-Amz-Expires=300"))
        assertEquals("image/png", upload.headers["Content-Type"])
        assertEquals("public, max-age=31536000, immutable", upload.headers["Cache-Control"])
    }
}
