package com.ogonggo.core.advertisementexposure.domain

import com.ogonggo.core.enumeration.EnumField

/** 세팅 화면의 위로·아래로다. 같은 자리 안에서 바로 옆 노출과 순서를 바꾼다. */
enum class AdvertisementExposureMoveDirection(
    override val code: Int,
    override val desc: String,
) : EnumField {
    UP(1, "위로"),
    DOWN(2, "아래로"),
}
