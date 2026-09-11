package com.ogonggo.userapi.image.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
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
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "이미지")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface ImageUploadApi {

    @Operation(
        operationId = "createImage",
        summary = "이미지 업로드",
        description = "이미지 파일을 S3에 저장하고 Lexical image 노드에 사용할 표시용 URL을 반환합니다.",
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
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>>
}
