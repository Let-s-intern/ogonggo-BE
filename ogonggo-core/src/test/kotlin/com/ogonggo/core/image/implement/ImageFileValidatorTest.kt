package com.ogonggo.core.image.implement

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ImageFileValidatorTest {

    private val validator = ImageFileValidator()

    @ParameterizedTest
    @ValueSource(strings = ["png", "jpg", "webp"])
    fun `허용된 이미지 포맷은 실제 파일 시그니처를 기준으로 저장 정보를 만든다`(format: String) {
        val result = validator.validate(ImageUploadDto(signature(format)))

        assertEquals(
            when (format) {
                "jpg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "image/webp"
            },
            result.mimeType,
        )
        assertEquals(format, result.extension)
    }

    @ParameterizedTest
    @ValueSource(strings = ["png", "jpg"])
    fun `푸시 알림 용도로 JPEG와 PNG 이미지를 올릴 수 있다`(format: String) {
        // given
        val image = ImageUploadDto(signature(format), ImageAssetUsage.NOTIFICATION)

        // when
        val result = validator.validate(image)

        // then
        assertEquals(if (format == "png") "image/png" else "image/jpeg", result.mimeType)
    }

    @Test
    fun `푸시 알림 용도로 WebP 이미지를 올릴 수 없다`() {
        // given
        val image = ImageUploadDto(signature("webp"), ImageAssetUsage.NOTIFICATION)

        // when
        val exception = assertThrows(InvalidValueException::class.java) { validator.validate(image) }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED, exception.errorCode)
    }

    @ParameterizedTest
    @ValueSource(strings = ["<svg><script>alert(1)</script></svg>", "not-an-image"])
    fun `허용되지 않은 파일은 거부한다`(content: String) {
        val exception = assertThrows(InvalidValueException::class.java) {
            validator.validate(ImageUploadDto(content.toByteArray()))
        }

        assertEquals(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED, exception.errorCode)
    }

    @Test
    fun `빈 파일은 필수 파일 오류로 거부한다`() {
        val exception = assertThrows(InvalidValueException::class.java) {
            validator.validate(ImageUploadDto(byteArrayOf()))
        }

        assertEquals(ImageUploadErrorCode.IMAGE_FILE_REQUIRED, exception.errorCode)
    }

    @Test
    fun `파일 크기가 10 MiB를 초과하면 거부한다`() {
        val exception = assertThrows(InvalidValueException::class.java) {
            validator.validate(ImageUploadDto(ByteArray(10 * 1024 * 1024 + 1)))
        }

        assertEquals(ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE, exception.errorCode)
    }

    private fun signature(format: String): ByteArray = when (format) {
        "jpg" -> byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        "png" -> byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        "webp" -> byteArrayOf(
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50,
        )
        else -> error("지원하지 않는 테스트 포맷입니다: $format")
    }
}
