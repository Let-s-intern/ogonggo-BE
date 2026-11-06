package com.ogonggo.core.adchannelformat.implement.dto

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormatTerms

data class AdChannelFormatAppendDto(
    val platform: AdChannelPlatform,
    val terms: AdChannelFormatTerms,
)
