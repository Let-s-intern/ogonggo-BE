package com.ogonggo.adminapi.channelad.presentation.response

import com.ogonggo.adminapi.channelad.business.AdminChannelAdResult
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminChannelAdResponse(
    val id: Long,
    val advertiserId: Long,
    @Schema(description = "광고주 요약. 광고주를 찾을 수 없으면 null입니다.")
    val advertiser: AdminChannelAdAdvertiserSummaryResponse?,
    val contractId: Long?,
    @Schema(description = "광고 계약 요약. 계약이 없으면 null입니다.")
    val contract: AdminChannelAdContractSummaryResponse?,
    @Schema(description = "채널의 플랫폼")
    val platform: AdChannelPlatform,
    val channelId: Long,
    @Schema(description = "채널 요약. 채널을 찾을 수 없으면 null입니다.")
    val channel: AdminChannelAdChannelSummaryResponse?,
    val formatId: Long,
    @Schema(description = "게재 유형 요약. 유형을 찾을 수 없으면 null입니다.")
    val format: AdminChannelAdFormatSummaryResponse?,
    @Schema(description = "게시 예정일(KST). 게시 뒤에는 실제 게시일입니다.")
    val postDate: LocalDate,
    @Schema(description = "노출 끝 날(KST, 그날 포함). 하루짜리 게시면 null입니다.")
    val endDate: LocalDate?,
    val status: ChannelAdStatus,
    @Schema(description = "게시 확인 주소")
    val postUrl: String?,
    @Schema(description = "소재·문구 메모")
    val creativeMemo: String?,
    @Schema(description = "담당자 메모")
    val adminMemo: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminChannelAdResult): AdminChannelAdResponse = AdminChannelAdResponse(
            id = result.id,
            advertiserId = result.advertiserId,
            advertiser = result.advertiser?.let { AdminChannelAdAdvertiserSummaryResponse(it.id, it.name, it.type) },
            contractId = result.contractId,
            contract = result.contract?.let { AdminChannelAdContractSummaryResponse(it.id, it.name, it.startAt, it.endAt) },
            platform = result.platform,
            channelId = result.channelId,
            channel = result.channel?.let { AdminChannelAdChannelSummaryResponse(it.id, it.platform, it.name, it.handle) },
            formatId = result.formatId,
            format = result.format?.let { AdminChannelAdFormatSummaryResponse(it.id, it.platform, it.name) },
            postDate = result.postDate,
            endDate = result.endDate,
            status = result.status,
            postUrl = result.postUrl,
            creativeMemo = result.creativeMemo,
            adminMemo = result.adminMemo,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class AdminChannelAdAdvertiserSummaryResponse(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)

data class AdminChannelAdContractSummaryResponse(
    val id: Long,
    @Schema(description = "계약명. 없으면 null입니다.")
    val name: String?,
    @Schema(description = "광고 시작(KST, 포함)")
    val startAt: LocalDateTime,
    @Schema(description = "광고 끝(KST, 미포함)")
    val endAt: LocalDateTime,
)

data class AdminChannelAdChannelSummaryResponse(
    val id: Long,
    val platform: AdChannelPlatform,
    val name: String,
    @Schema(description = "인스타그램 계정 이름(@ 없이). 오픈채팅은 null입니다.")
    val handle: String?,
)

data class AdminChannelAdFormatSummaryResponse(
    val id: Long,
    val platform: AdChannelPlatform,
    val name: String,
)

/** 지운 채널 광고의 식별자다. */
data class AdminChannelAdIdResponse(
    val id: Long,
)
