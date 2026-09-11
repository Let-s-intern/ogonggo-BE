package com.ogonggo.userapi.image.presentation

import com.ogonggo.userapi.image.business.ImageUploadService
import com.ogonggo.userapi.image.business.UploadImageCommand
import com.ogonggo.userapi.image.presentation.response.ImageUploadResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
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
    ): ResponseEntity<SuccessResponse<ImageUploadResponse>> {
        val result = imageUploadService.upload(
            userId = userId,
            command = UploadImageCommand(
                content = file.bytes,
            ),
        )
        return SuccessResponse.created(ImageUploadResponse.from(result))
    }
}
