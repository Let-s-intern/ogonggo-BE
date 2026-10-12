package com.ogonggo.userapi.image.presentation.request

import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.dto.ImagePresignDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CreateImagePresignedUrlRequest(
    @field:Schema(description = "이미지를 쓸 곳. S3 폴더(ogonggo/profile, ogonggo/side, ogonggo/concern)를 정합니다.")
    val usage: ImageAssetUsage,
    @field:Schema(description = "올릴 파일의 MIME 타입", allowableValues = ["image/jpeg", "image/png", "image/webp"])
    @field:NotBlank
    val contentType: String,
    @field:Schema(description = "올릴 파일 크기(바이트). 10 MiB 이하", example = "1153433")
    @field:Positive
    val size: Long,
) {
    fun toCommand(): ImagePresignDto = ImagePresignDto(usage = usage, mimeType = contentType, size = size)
}
