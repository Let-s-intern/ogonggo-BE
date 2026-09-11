package com.ogonggo.core.storage.s3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class S3ImageStorageTest {

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
        val storage = S3ImageStorage(
            s3Client = s3Client,
            properties = S3ImageStorageProperties(
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
        val storage = S3ImageStorage(
            s3Client = s3Client,
            properties = S3ImageStorageProperties(),
        )

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            storage.put("images/image-id.png", byteArrayOf(1), "image/png")
        }
        Mockito.verifyNoInteractions(s3Client)
    }
}
