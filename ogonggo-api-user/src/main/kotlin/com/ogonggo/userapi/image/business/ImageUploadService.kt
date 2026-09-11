package com.ogonggo.userapi.image.business

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.storage.s3.S3ImageStorage
import com.ogonggo.userapi.image.error.ImageUploadErrorCode
import com.ogonggo.userapi.image.implement.ImageFileValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImageUploadService(
    private val imageFileValidator: ImageFileValidator,
    private val s3ImageStorage: S3ImageStorage,
) {

    fun upload(userId: Long, command: UploadImageCommand): ImageUploadResult {
        val image = imageFileValidator.validate(command)
        val imageId = UUID.randomUUID().toString()
        val key = "images/$imageId.${image.extension}"

        val url = try {
            s3ImageStorage.put(
                key = key,
                content = image.content,
                contentType = image.mimeType,
            )
        } catch (exception: Exception) {
            log.error("이미지 S3 업로드에 실패했습니다. userId={}, imageId={}", userId, imageId, exception)
            throw InternalServerException(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED)
        }

        return ImageUploadResult(
            id = imageId,
            url = url,
            mimeType = image.mimeType,
            size = image.content.size.toLong(),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ImageUploadService::class.java)
    }
}

data class UploadImageCommand(
    val content: ByteArray,
)

data class ImageUploadResult(
    val id: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)
