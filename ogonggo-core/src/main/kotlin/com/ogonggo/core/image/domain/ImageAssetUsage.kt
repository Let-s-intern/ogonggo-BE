package com.ogonggo.core.image.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 이미지를 어디에 쓰려고 올리는지다. S3에서 `ogonggo/{directory}/` 아래로 나눠 두고, 받을 수 있는 형식을 정한다.
 * 이미지를 실제로 어디에 붙였는지는 쓰는 쪽이 이미지 식별자로 기억한다.
 */
enum class ImageAssetUsage(
    override val code: Int,
    override val desc: String,
    val directory: String,
    val supportedMimeTypes: Set<String>,
) : EnumField {
    PROFILE(1, "프로필 이미지·기업 로고", "profile", ALL_FORMATS),
    RECRUITMENT_POST(2, "사이드 프로젝트·스터디 모집글", "side", setOf(JPEG, PNG)),
    CONCERN(3, "취준고민", "concern", setOf(JPEG, PNG)),
}

private const val JPEG = "image/jpeg"
private const val PNG = "image/png"
private const val WEBP = "image/webp"
private val ALL_FORMATS = setOf(JPEG, PNG, WEBP)
