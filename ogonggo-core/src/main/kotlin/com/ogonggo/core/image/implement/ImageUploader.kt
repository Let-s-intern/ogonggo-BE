package com.ogonggo.core.image.implement

import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 이미지 검증과 S3 저장을 조율하는 공통 Implement다.
 * HTTP 요청, 인증 주체, API별 오류 응답은 알지 못한다.
 */
@Component
class ImageUploader internal constructor(
    private val imageFileValidator: ImageFileValidator,
    private val imageAssetManager: ImageAssetManager,
    private val s3ObjectClient: S3ObjectClient,
) {

    fun upload(ownerUserId: Long, command: ImageUploadDto): ImageUploadResultDto {
        val image = imageFileValidator.validate(command)
        val imageId = UUID.randomUUID().toString()
        val key = "images/$imageId.${image.extension}"
        val url = s3ObjectClient.urlOf(key)
        imageAssetManager.startUploading(
            id = imageId,
            ownerUserId = ownerUserId,
            storageKey = key,
            url = url,
            mimeType = image.mimeType,
            size = image.content.size.toLong(),
        )
        s3ObjectClient.put(
            key = key,
            content = image.content,
            contentType = image.mimeType,
        )
        imageAssetManager.markUploaded(imageId)

        return ImageUploadResultDto(
            id = imageId,
            url = url,
            mimeType = image.mimeType,
            size = image.content.size.toLong(),
        )
    }
}
