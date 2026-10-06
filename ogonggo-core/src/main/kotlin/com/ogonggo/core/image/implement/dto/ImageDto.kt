package com.ogonggo.core.image.implement.dto

data class ImageUploadDto(
    val content: ByteArray,
)

data class ImageUploadResultDto(
    val id: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)
