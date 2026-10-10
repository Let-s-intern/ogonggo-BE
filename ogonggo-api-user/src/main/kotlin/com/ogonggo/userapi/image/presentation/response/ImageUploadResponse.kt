package com.ogonggo.userapi.image.presentation.response

import com.ogonggo.core.image.implement.dto.ImageUploadResultDto

data class ImageUploadResponse(
    val id: String,
    val url: String,
    val mimeType: String,
    val size: Long,
) {
    companion object {
        fun from(result: ImageUploadResultDto): ImageUploadResponse = ImageUploadResponse(
            id = result.id,
            url = result.url,
            mimeType = result.mimeType,
            size = result.size,
        )
    }
}
