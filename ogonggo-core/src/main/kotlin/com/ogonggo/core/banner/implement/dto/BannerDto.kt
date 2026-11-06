package com.ogonggo.core.banner.implement.dto

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.domain.BannerSettings

/** 등록할 배너다. 그 자리 맨 뒤에 붙는다. */
data class BannerAppendDto(
    val placement: BannerPlacement,
    val settings: BannerSettings,
)
