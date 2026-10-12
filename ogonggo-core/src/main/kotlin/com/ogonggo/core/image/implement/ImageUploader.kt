package com.ogonggo.core.image.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.dto.ImagePresignDto
import com.ogonggo.core.image.implement.dto.ImagePresignedUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 이미지 검증과 S3 저장을 조율하는 공통 Implement다.
 * HTTP 요청, 인증 주체, API별 오류 응답은 알지 못한다.
 */
@Component
class ImageUploader internal constructor(
    private val imageFileValidator: ImageFileValidator,
    private val imageAssetReader: ImageAssetReader,
    private val imageAssetManager: ImageAssetManager,
    private val s3ObjectClient: S3ObjectClient,
) {

    fun upload(ownerUserId: Long, command: ImageUploadDto): ImageUploadResultDto {
        val image = imageFileValidator.validate(command)
        val imageId = UUID.randomUUID().toString()
        val key = storageKey(command.usage, imageId, image.extension)
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

    /**
     * 클라이언트가 S3에 직접 올릴 presigned PUT URL을 만든다. 이미지는 업로드 중 상태로 먼저 기록하고,
     * 클라이언트가 올린 뒤 [completeUpload]를 불러야 본문·프로필에 쓸 수 있다. 완료하지 않으면 정리 작업이 지운다.
     */
    fun issueUploadUrl(ownerUserId: Long, command: ImagePresignDto): ImagePresignedUploadDto {
        val image = imageFileValidator.validateDeclared(command.usage, command.mimeType, command.size)
        val imageId = UUID.randomUUID().toString()
        val key = storageKey(command.usage, imageId, image.extension)
        val url = s3ObjectClient.urlOf(key)
        val upload = s3ObjectClient.presignPut(key, image.mimeType, command.size, UPLOAD_URL_EXPIRES_IN)
        imageAssetManager.startUploading(
            id = imageId,
            ownerUserId = ownerUserId,
            storageKey = key,
            url = url,
            mimeType = image.mimeType,
            size = command.size,
        )
        return ImagePresignedUploadDto(id = imageId, uploadUrl = upload.url, uploadHeaders = upload.headers, url = url)
    }

    /**
     * presigned URL로 올린 파일이 알린 크기·형식과 같은지 S3에서 확인하고 임시 상태로 바꾼다.
     * 이미 완료한 이미지를 다시 완료해도 같은 결과를 돌려준다.
     */
    fun completeUpload(ownerUserId: Long, imageId: String): ImageUploadResultDto {
        val asset = imageAssetReader.readOwned(ownerUserId, imageId)
        when (asset.status) {
            ImageAssetStatus.UPLOADING -> verifyUploaded(asset)
            ImageAssetStatus.TEMPORARY, ImageAssetStatus.ATTACHED, ImageAssetStatus.UNREFERENCED -> return asset.toResult()
            ImageAssetStatus.DELETE_PENDING, ImageAssetStatus.DELETED ->
                throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }
        imageAssetManager.markUploaded(imageId)
        return asset.toResult()
    }

    private fun verifyUploaded(asset: ImageAsset) {
        val size = s3ObjectClient.sizeOf(asset.storageKey)
            ?: throw ConflictException(ImageUploadErrorCode.IMAGE_FILE_NOT_UPLOADED)
        // 크기는 presigned URL 서명에 묶여 다를 수 없지만, 다르면 쓸 수 없는 이미지로 본다.
        if (size != asset.size) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }
        imageFileValidator.verifyUploaded(
            s3ObjectClient.readHead(asset.storageKey, ImageFileValidator.SIGNATURE_LENGTH),
            asset.mimeType,
        )
    }

    /** 렛츠커리어와 같이 쓰는 버킷이라 오공고 이미지는 `ogonggo/` 아래에 용도별로 나눠 둔다. */
    private fun storageKey(usage: ImageAssetUsage?, imageId: String, extension: String): String =
        "$KEY_PREFIX/${usage?.directory ?: ETC_DIRECTORY}/$imageId.$extension"

    private fun ImageAsset.toResult(): ImageUploadResultDto =
        ImageUploadResultDto(id = id, url = url, mimeType = mimeType, size = size)

    private companion object {
        const val KEY_PREFIX = "ogonggo"
        const val ETC_DIRECTORY = "etc"
        val UPLOAD_URL_EXPIRES_IN: Duration = Duration.ofMinutes(5)
    }
}
