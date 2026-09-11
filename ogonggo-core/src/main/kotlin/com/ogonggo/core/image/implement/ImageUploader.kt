package com.ogonggo.core.image.implement

import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import com.ogonggo.core.image.implement.dto.ImageUploadResult
import com.ogonggo.core.storage.s3.S3ImageStorage
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 이미지 검증과 S3 저장을 조율하는 공통 Implement다.
 * HTTP 요청, 인증 주체, API별 오류 응답은 알지 못한다.
 */
@Component
class ImageUploader internal constructor(
    private val imageFileValidator: ImageFileValidator,
    private val s3ImageStorage: S3ImageStorage,
) {

    fun upload(command: ImageUploadCommand): ImageUploadResult {
        val image = imageFileValidator.validate(command)
        val imageId = UUID.randomUUID().toString()
        val key = "images/$imageId.${image.extension}"
        val url = s3ImageStorage.put(
            key = key,
            content = image.content,
            contentType = image.mimeType,
        )

        return ImageUploadResult(
            id = imageId,
            url = url,
            mimeType = image.mimeType,
            size = image.content.size.toLong(),
        )
    }
}
