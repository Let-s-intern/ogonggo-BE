package com.ogonggo.core.image.implement.dto

import com.ogonggo.core.image.domain.ImageAssetUsage

data class ImageUploadDto(
    val content: ByteArray,
    /** 보내지 않으면 `ogonggo/etc` 아래에 둔다. multipart 업로드를 쓰는 기존 화면이 용도를 보내지 않아서다. */
    val usage: ImageAssetUsage? = null,
)

data class ImageUploadResultDto(
    val id: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)

data class ImagePresignDto(
    val usage: ImageAssetUsage,
    val mimeType: String,
    val size: Long,
)

data class ImagePresignedUploadDto(
    val id: String,
    val uploadUrl: String,
    val uploadHeaders: Map<String, String>,
    val url: String,
)
