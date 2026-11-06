package com.ogonggo.adminapi.adchannel.presentation.response

import com.ogonggo.adminapi.adchannel.business.AdminAdChannelResult
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdChannelResponse(
    val id: Long,
    val platform: AdChannelPlatform,
    @Schema(description = "표시 이름. 오픈채팅은 방 이름")
    val name: String,
    @Schema(description = "인스타그램 계정 이름(@ 없이). 오픈채팅은 null입니다.")
    val handle: String?,
    @Schema(description = "인스타그램 프로필 주소, 오픈채팅방 참여 주소")
    val url: String,
    @Schema(description = "프로필 이미지. https 주소이거나 웹 사이트 경로. 없으면 null입니다.")
    val profileImageUrl: String?,
    @Schema(description = "오픈채팅 참여 인원, 인스타그램 팔로워 수. 없으면 null입니다.")
    val memberCount: Int?,
    @Schema(description = "끄면 새 채널 광고에서 고를 수 없습니다.")
    val enabled: Boolean,
    val memo: String?,
    @Schema(description = "이 채널에 잡힌 채널 광고 수. 1 이상이면 지울 수 없습니다.")
    val adCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdChannelResult): AdminAdChannelResponse = AdminAdChannelResponse(
            id = result.id,
            platform = result.platform,
            name = result.name,
            handle = result.handle,
            url = result.url,
            profileImageUrl = result.profileImageUrl,
            memberCount = result.memberCount,
            enabled = result.enabled,
            memo = result.memo,
            adCount = result.adCount,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 지운 채널의 식별자다. */
data class AdminAdChannelIdResponse(
    val id: Long,
)
