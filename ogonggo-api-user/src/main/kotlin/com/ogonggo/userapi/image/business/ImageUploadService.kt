package com.ogonggo.userapi.image.business

import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.ImageUploader
import com.ogonggo.core.image.implement.dto.ImagePresignDto
import com.ogonggo.core.image.implement.dto.ImagePresignedUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ImageUploadService(
    private val imageUploader: ImageUploader,
) {

    fun upload(userId: Long, command: ImageUploadDto): ImageUploadResultDto {
        return try {
            imageUploader.upload(userId, command)
        } catch (exception: BusinessException) {
            throw exception
        } catch (exception: Exception) {
            log.error("이미지 S3 업로드에 실패했습니다. userId={}", userId, exception)
            throw InternalServerException(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED)
        }
    }

    fun issueUploadUrl(userId: Long, command: ImagePresignDto): ImagePresignedUploadDto =
        withS3Failure(userId) { imageUploader.issueUploadUrl(userId, command) }

    fun completeUpload(userId: Long, imageId: String): ImageUploadResultDto =
        withS3Failure(userId) { imageUploader.completeUpload(userId, imageId) }

    private fun <T> withS3Failure(userId: Long, block: () -> T): T =
        try {
            block()
        } catch (exception: BusinessException) {
            throw exception
        } catch (exception: Exception) {
            log.error("이미지 S3 작업에 실패했습니다. userId={}", userId, exception)
            throw InternalServerException(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED)
        }

    companion object {
        private val log = LoggerFactory.getLogger(ImageUploadService::class.java)
    }
}
