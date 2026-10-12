package com.ogonggo.userapi.image.presentation.response

import com.ogonggo.core.image.implement.dto.ImagePresignedUploadDto
import io.swagger.v3.oas.annotations.media.Schema

data class ImagePresignedUrlResponse(
    @field:Schema(description = "이미지 식별자. 업로드 완료와 본문 이미지 노드의 imageId에 씁니다.")
    val id: String,
    @field:Schema(description = "파일을 PUT으로 올릴 S3 주소. 5분 동안 유효합니다.")
    val uploadUrl: String,
    @field:Schema(description = "PUT 요청에 그대로 넣어야 하는 헤더. 서명에 묶여 있어 빠지거나 다르면 S3가 거절합니다.")
    val uploadHeaders: Map<String, String>,
    @field:Schema(description = "올린 뒤 화면에 보여 줄 주소. 본문 이미지 노드의 src에 씁니다.")
    val url: String,
) {
    companion object {
        fun from(result: ImagePresignedUploadDto): ImagePresignedUrlResponse = ImagePresignedUrlResponse(
            id = result.id,
            uploadUrl = result.uploadUrl,
            uploadHeaders = result.uploadHeaders,
            url = result.url,
        )
    }
}
