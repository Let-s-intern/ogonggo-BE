package com.ogonggo.core.image.implement

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import org.springframework.stereotype.Component

/**
 * 공통 이미지 업로드 정책을 검증한다.
 *
 * 이미지 디코딩과 해상도 제한은 초기 업로드 범위에서 제외한다.
 * 파일명·확장자·클라이언트 MIME 타입은 저장 포맷을 결정하는 데 사용하지 않는다.
 */
@Component
internal class ImageFileValidator {

    fun validate(command: ImageUploadCommand): ValidatedImage {
        if (command.content.isEmpty()) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_REQUIRED)
        }
        if (command.content.size > MAX_FILE_SIZE_BYTES) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE)
        }

        val format = detectFormat(command.content)
            ?: throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED)

        return ValidatedImage(
            content = command.content,
            mimeType = format.mimeType,
            extension = format.extension,
        )
    }

    private fun detectFormat(content: ByteArray): ImageFormat? = when {
        content.startsWith(PNG_SIGNATURE) -> ImageFormat.PNG
        content.startsWith(JPEG_SIGNATURE) -> ImageFormat.JPEG
        content.startsWith(RIFF_SIGNATURE) && content.startsWith(WEBP_SIGNATURE, 8) -> ImageFormat.WEBP
        else -> null
    }

    private enum class ImageFormat(
        val mimeType: String,
        val extension: String,
    ) {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    private companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024
        val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        val RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    }
}

data class ValidatedImage(
    val content: ByteArray,
    val mimeType: String,
    val extension: String,
)

private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean =
    offset >= 0 && size >= offset + prefix.size &&
        prefix.indices.all { index -> this[offset + index] == prefix[index] }
