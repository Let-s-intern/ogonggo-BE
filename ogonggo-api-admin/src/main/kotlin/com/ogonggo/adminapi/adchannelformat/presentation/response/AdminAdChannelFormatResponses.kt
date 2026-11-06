package com.ogonggo.adminapi.adchannelformat.presentation.response

import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatResult
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdChannelFormatResponse(
    val id: Long,
    val platform: AdChannelPlatform,
    @Schema(description = "이름. 같은 플랫폼 안에서 겹치지 않습니다.")
    val name: String,
    val description: String?,
    @Schema(description = "끄면 새 채널 광고와 계약 상품에서 고를 수 없습니다.")
    val enabled: Boolean,
    @Schema(description = "기본 단가(원). 안내용입니다. 없으면 null입니다.")
    val defaultPrice: Long?,
    @Schema(description = "이 유형으로 올릴 수 있는 채널(등록 순). 비어 있으면 그 플랫폼의 모든 채널입니다.")
    val channelIds: List<Long>,
    @Schema(description = "플랫폼 안 순서. 1부터")
    val displayOrder: Int,
    @Schema(description = "이 유형으로 잡힌 채널 광고 수. 1 이상이면 지울 수 없습니다.")
    val adCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdChannelFormatResult): AdminAdChannelFormatResponse = AdminAdChannelFormatResponse(
            id = result.id,
            platform = result.platform,
            name = result.name,
            description = result.description,
            enabled = result.enabled,
            defaultPrice = result.defaultPrice,
            channelIds = result.channelIds,
            displayOrder = result.displayOrder,
            adCount = result.adCount,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 지운 게재 유형의 식별자다. */
data class AdminAdChannelFormatIdResponse(
    val id: Long,
)
