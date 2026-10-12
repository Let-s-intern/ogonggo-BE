package com.ogonggo.userapi.image.presentation

import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.image.presentation.request.CreateImagePresignedUrlRequest
import com.ogonggo.userapi.image.presentation.response.ImagePresignedUrlResponse
import com.ogonggo.userapi.image.presentation.response.ImageUploadResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "이미지")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface ImageUploadApi {

    @Operation(
        operationId = "createImage",
        summary = "이미지 업로드",
        description = "이미지 파일을 S3에 임시 저장하고, 게시글 본문이나 프로필 이미지에 연결할 이미지 식별자와 표시용 URL을 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "업로드 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "IMAGE_FILE_REQUIRED, IMAGE_FILE_TYPE_NOT_SUPPORTED 또는 IMAGE_FILE_TOO_LARGE",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "IMAGE_UPLOAD_FAILED: 이미지 업로드 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun uploadImage(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "업로드할 이미지 파일", required = true) file: MultipartFile,
        @Parameter(description = "이미지를 쓸 곳. S3 폴더를 정하며, 보내지 않으면 ogonggo/etc에 둡니다.") usage: ImageAssetUsage?,
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>>

    @Operation(
        operationId = "createImagePresignedUrl",
        summary = "이미지 업로드 URL 발급",
        description = """
            파일을 서버를 거치지 않고 S3에 직접 올릴 presigned PUT URL을 발급합니다.
            1. 이 API로 `id`, `uploadUrl`, `uploadHeaders`, `url`을 받습니다.
            2. `uploadUrl`에 `uploadHeaders`를 그대로 넣어 파일을 PUT합니다. 5분 안에 올려야 하고, 알린 크기와 다르면 S3가 거절합니다.
            3. `POST /api/v1/images/{imageId}/complete`로 완료해야 본문·프로필에 쓸 수 있습니다. 완료하지 않은 이미지는 정리 작업이 지웁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "발급 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST, IMAGE_FILE_REQUIRED, IMAGE_FILE_TYPE_NOT_SUPPORTED 또는 IMAGE_FILE_TOO_LARGE",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "IMAGE_UPLOAD_FAILED: URL 발급 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createPresignedUrl(
        @Parameter(hidden = true) userId: Long,
        @Valid request: CreateImagePresignedUrlRequest,
    ): ResponseEntity<SuccessResponse<ImagePresignedUrlResponse>>

    @Operation(
        operationId = "completeImageUpload",
        summary = "이미지 업로드 완료",
        description = "presigned URL로 올린 파일이 알린 크기·형식과 같은지 확인하고 쓸 수 있는 이미지로 바꿉니다. 이미 완료한 이미지를 다시 완료해도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "완료", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = """
                    IMAGE_ASSET_NOT_FOUND: 내가 발급받은 이미지가 아님
                    IMAGE_FILE_TYPE_NOT_SUPPORTED: 올린 파일이 알린 형식의 이미지가 아님
                    IMAGE_ASSET_NOT_AVAILABLE: 이미 정리된 이미지
                """,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "IMAGE_FILE_NOT_UPLOADED: 아직 S3에 파일을 올리지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "IMAGE_UPLOAD_FAILED: S3 확인 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun completeUpload(
        @Parameter(hidden = true) userId: Long,
        imageId: String,
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>>
}
