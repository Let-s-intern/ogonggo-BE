package com.ogonggo.userapi.image.business

import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.ImageUploader
import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import com.ogonggo.core.image.implement.dto.ImageUploadResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ImageUploadService(
    private val imageUploader: ImageUploader,
) {

    fun upload(userId: Long, command: ImageUploadCommand): ImageUploadResult {
        return try {
            imageUploader.upload(command)
        } catch (exception: BusinessException) {
            throw exception
        } catch (exception: Exception) {
            log.error("이미지 S3 업로드에 실패했습니다. userId={}", userId, exception)
            throw InternalServerException(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ImageUploadService::class.java)
    }
}
