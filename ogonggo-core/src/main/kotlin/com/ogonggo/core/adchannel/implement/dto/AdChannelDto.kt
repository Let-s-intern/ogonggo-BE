package com.ogonggo.core.adchannel.implement.dto

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannel.domain.AdChannelProfile

data class AdChannelAppendDto(
    val platform: AdChannelPlatform,
    val profile: AdChannelProfile,
)
