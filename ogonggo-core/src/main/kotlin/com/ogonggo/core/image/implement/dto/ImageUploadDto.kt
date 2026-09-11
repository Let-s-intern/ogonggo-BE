package com.ogonggo.core.image.implement.dto

data class ImageUploadCommand(
    val content: ByteArray,
)

data class ImageUploadResult(
    val id: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)
