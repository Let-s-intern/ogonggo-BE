package com.ogonggo.userapi.image.presentation

import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.userapi.image.business.ImageUploadService
import com.ogonggo.userapi.image.presentation.request.CreateImagePresignedUrlRequest
import com.ogonggo.userapi.image.presentation.response.ImagePresignedUrlResponse
import com.ogonggo.userapi.image.presentation.response.ImageUploadResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/images")
class ImageUploadController(
    private val imageUploadService: ImageUploadService,
) : ImageUploadApi {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun uploadImage(
        @AuthenticationPrincipal userId: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam("usage", required = false) usage: ImageAssetUsage?,
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>> {
        val result = imageUploadService.upload(
            userId = userId,
            command = ImageUploadDto(
                content = file.bytes,
                usage = usage,
            ),
        )
        return SuccessResponse.created(ImageUploadResponse.from(result))
    }

    @PostMapping("/presigned-urls")
    override fun createPresignedUrl(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: CreateImagePresignedUrlRequest,
    ): ResponseEntity<SuccessResponse<ImagePresignedUrlResponse>> =
        SuccessResponse.created(
            ImagePresignedUrlResponse.from(imageUploadService.issueUploadUrl(userId, request.toCommand())),
        )

    @PostMapping("/{imageId}/complete")
    override fun completeUpload(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("imageId") imageId: String,
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>> =
        SuccessResponse.ok(ImageUploadResponse.from(imageUploadService.completeUpload(userId, imageId)))
}
