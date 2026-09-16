package com.ogonggo.userapi.image.business

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.ImageUploader
import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import com.ogonggo.core.image.implement.dto.ImageUploadResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class ImageUploadServiceTest {

    private val imageUploader = Mockito.mock(ImageUploader::class.java)
    private val service = ImageUploadService(imageUploader)

    @Test
    fun `공통 이미지 업로더를 호출하고 업로드 결과를 반환한다`() {
        val content = byteArrayOf(1, 2, 3)
        val command = ImageUploadCommand(content)
        val result = ImageUploadResult(
            id = "image-id",
            url = "https://cdn.example.com/images/image.png",
            mimeType = "image/png",
            size = 3L,
        )
        Mockito.`when`(imageUploader.upload(17L, command)).thenReturn(result)

        val uploaded = service.upload(17L, command)

        assertEquals(result, uploaded)
        Mockito.verify(imageUploader).upload(17L, command)
    }

    @Test
    fun `공통 업로더의 이미지 검증 오류는 그대로 전달한다`() {
        val command = ImageUploadCommand(byteArrayOf())
        val exception = InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_REQUIRED)
        Mockito.`when`(imageUploader.upload(17L, command)).thenThrow(exception)

        val thrown = assertThrows(InvalidValueException::class.java) {
            service.upload(17L, command)
        }

        assertSame(exception, thrown)
    }

    @Test
    fun `공통 업로더의 저장 오류는 사용자 API 오류로 변환한다`() {
        val command = ImageUploadCommand(byteArrayOf(1, 2, 3))
        Mockito.`when`(imageUploader.upload(17L, command)).thenThrow(IllegalStateException("S3 unavailable"))

        val thrown = assertThrows(InternalServerException::class.java) {
            service.upload(17L, command)
        }

        assertEquals(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED, thrown.errorCode)
    }
}
