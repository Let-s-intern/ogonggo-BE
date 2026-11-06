package com.ogonggo.core.channelad.domain

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import java.time.LocalDate

/**
 * 채널 광고 목록 조건이다. 모두 비우면 전부다. 기간은 광고가 차지하는 날(`postDate` ~ `endDate ?? postDate`, 둘 다 포함)이
 * `[from, toExclusive)`와 겹치는 것이다. 한쪽만 주면 그쪽만 본다.
 */
data class ChannelAdSearchCondition(
    val from: LocalDate? = null,
    val toExclusive: LocalDate? = null,
    val advertiserId: Long? = null,
    val contractId: Long? = null,
    val platform: AdChannelPlatform? = null,
    val channelId: Long? = null,
    val formatId: Long? = null,
    val status: ChannelAdStatus? = null,
)
