package com.ogonggo.core.image.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 이미지를 어디에 쓰려고 올리는지다. S3에서 `ogonggo/{directory}/` 아래로 나눠 두는 데만 쓰고,
 * 이미지를 실제로 어디에 붙일 수 있는지는 정하지 않는다. 쓰는 곳은 쓰는 쪽이 이미지 식별자로 기억한다.
 */
enum class ImageAssetUsage(
    override val code: Int,
    override val desc: String,
    val directory: String,
) : EnumField {
    PROFILE(1, "프로필 이미지·기업 로고", "profile"),
    RECRUITMENT_POST(2, "사이드 프로젝트·스터디 모집글", "side"),
    CONCERN(3, "취준고민", "concern"),
}
