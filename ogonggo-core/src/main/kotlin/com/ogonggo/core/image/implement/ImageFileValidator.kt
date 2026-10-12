package com.ogonggo.core.image.implement

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import org.springframework.stereotype.Component

/**
 * 공통 이미지 업로드 정책을 검증한다.
 *
 * 이미지 디코딩과 해상도 제한은 초기 업로드 범위에서 제외한다.
 * 파일명·확장자·클라이언트 MIME 타입은 저장 포맷을 결정하는 데 사용하지 않는다.
 */
@Component
internal class ImageFileValidator {

    fun validate(command: ImageUploadDto): ValidatedImage {
        if (command.content.isEmpty()) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_REQUIRED)
        }
        if (command.content.size > MAX_FILE_SIZE_BYTES) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE)
        }

        val format = detectFormat(command.content)
            ?.takeIf { command.usage == null || it.mimeType in command.usage.supportedMimeTypes }
            ?: throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED)

        return ValidatedImage(
            content = command.content,
            mimeType = format.mimeType,
            extension = format.extension,
        )
    }

    /**
     * presigned URL로 올리기 전에 클라이언트가 알린 형식과 크기를 검증한다.
     * 실제 파일은 서버를 거치지 않으므로 업로드 완료 때 [verifyUploaded]로 다시 확인한다.
     */
    fun validateDeclared(usage: ImageAssetUsage, mimeType: String, size: Long): DeclaredImage {
        if (size <= 0) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_REQUIRED)
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE)
        }
        val format = ImageFormat.entries.firstOrNull { it.mimeType == mimeType.lowercase() }
            ?.takeIf { it.mimeType in usage.supportedMimeTypes }
            ?: throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED)
        return DeclaredImage(mimeType = format.mimeType, extension = format.extension)
    }

    /** S3에 올라온 파일의 앞부분 바이트가 알린 형식과 같은지 확인한다. */
    fun verifyUploaded(header: ByteArray, mimeType: String) {
        if (detectFormat(header)?.mimeType != mimeType) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED)
        }
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

    companion object {
        /** 형식을 판별하는 데 필요한 파일 앞부분 바이트 수다. WEBP가 12바이트로 가장 길다. */
        const val SIGNATURE_LENGTH = 12
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        private val RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        private val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    }
}

data class DeclaredImage(
    val mimeType: String,
    val extension: String,
)

data class ValidatedImage(
    val content: ByteArray,
    val mimeType: String,
    val extension: String,
)

private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean =
    offset >= 0 && size >= offset + prefix.size &&
        prefix.indices.all { index -> this[offset + index] == prefix[index] }
