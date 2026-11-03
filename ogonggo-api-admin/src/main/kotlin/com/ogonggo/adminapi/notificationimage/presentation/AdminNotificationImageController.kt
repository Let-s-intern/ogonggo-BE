package com.ogonggo.adminapi.notificationimage.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.ImageUploader
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "관리자 푸시 이미지")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
@RestController
@RequestMapping("/api/v1/admin/notification-images")
class AdminNotificationImageController(private val uploader: ImageUploader) {
    @Operation(operationId = "uploadNotificationImage", summary = "푸시 알림 이미지 업로드",
        description = "JPEG 또는 PNG 이미지를 업로드하고 발송 요청의 imageId에 쓸 식별자를 받습니다. 파일은 10 MiB 이하입니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "업로드 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "IMAGE_FILE_REQUIRED, IMAGE_FILE_TYPE_NOT_SUPPORTED, IMAGE_FILE_TOO_LARGE"),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
        ApiResponse(responseCode = "403", description = "FORBIDDEN"),
    ])
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @Parameter(hidden = true) @AuthenticationPrincipal adminUserId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<SuccessResponse<AdminNotificationImageResponse>> {
        val image = try {
            uploader.upload(adminUserId, ImageUploadDto(file.bytes, ImageAssetUsage.NOTIFICATION))
        } catch (exception: BusinessException) {
            throw exception
        } catch (exception: Exception) {
            log.error("관리자 푸시 이미지 업로드 실패. adminUserId={}", adminUserId, exception)
            throw InternalServerException(ImageUploadErrorCode.IMAGE_UPLOAD_FAILED)
        }
        return SuccessResponse.created(AdminNotificationImageResponse.from(image))
    }

    private companion object { val log = LoggerFactory.getLogger(AdminNotificationImageController::class.java) }
}

data class AdminNotificationImageResponse(val id: String, val url: String, val mimeType: String, val size: Long) {
    companion object {
        fun from(image: ImageUploadResultDto) = AdminNotificationImageResponse(image.id, image.url, image.mimeType, image.size)
    }
}
