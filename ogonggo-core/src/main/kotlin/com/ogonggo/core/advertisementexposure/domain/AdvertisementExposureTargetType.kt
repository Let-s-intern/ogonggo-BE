package com.ogonggo.core.advertisementexposure.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 공고 노출이 띄우는 콘텐츠의 종류다. */
enum class AdvertisementExposureTargetType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB(1, "채용공고"),
    BOOTCAMP(2, "부트캠프"),
}
