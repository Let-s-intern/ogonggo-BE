package com.ogonggo.adminapi.adchannelformat.presentation.request

import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatCreateCommand
import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatOrderCommand
import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatUpdateCommand
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminAdChannelFormatRequest(
    @Schema(description = "플랫폼. 등록한 뒤에는 바꾸지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val platform: AdChannelPlatform? = null,
    @Schema(description = "이름. 1~50자, 같은 플랫폼 안에서 겹치지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String? = null,
    @Schema(description = "설명. 500자 이하", nullable = true)
    val description: String? = null,
    @Schema(description = "끄면 새 채널 광고와 계약 상품에서 고를 수 없습니다. 보내지 않으면 켭니다.")
    val enabled: Boolean? = null,
    @Schema(description = "기본 단가(원). 0 이상 정수, 안내용이고 계약 금액과 상관없습니다.", nullable = true)
    val defaultPrice: Long? = null,
    @ArraySchema(
        arraySchema = Schema(description = "이 유형으로 올릴 수 있는 채널. 같은 플랫폼의 채널만 보냅니다. 비우면 그 플랫폼의 모든 채널(나중에 더한 채널 포함)입니다."),
    )
    val channelIds: List<Long?>? = null,
) {
    fun toCommand(): AdminAdChannelFormatCreateCommand = AdminAdChannelFormatCreateCommand(
        platform = platform,
        name = name,
        description = description,
        enabled = enabled,
        defaultPrice = defaultPrice,
        channelIds = channelIds,
    )
}

/**
 * 보낸 칸만 바꿉니다. `description`, `defaultPrice`는 null을 보내면 지웁니다. 그 밖의 칸은 null이면 보내지 않은 것과 같습니다.
 * `platform`을 저장된 값과 다르게 보내면 400입니다.
 */
data class UpdateAdminAdChannelFormatRequest(
    val platform: AdChannelPlatform? = null,
    val name: String? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 설명을 지웁니다.")
    val description: Optional<String>? = null,
    val enabled: Boolean? = null,
    @Schema(implementation = Long::class, nullable = true, description = "null을 보내면 기본 단가를 지웁니다.")
    val defaultPrice: Optional<Long>? = null,
    val channelIds: List<Long?>? = null,
) {
    fun toCommand(): AdminAdChannelFormatUpdateCommand = AdminAdChannelFormatUpdateCommand(
        platform = platform,
        name = name,
        description = description,
        enabled = enabled,
        defaultPrice = defaultPrice,
        channelIds = channelIds,
    )
}

data class ReplaceAdminAdChannelFormatOrderRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val platform: AdChannelPlatform? = null,
    @ArraySchema(arraySchema = Schema(description = "그 플랫폼의 유형 id를 바꿀 순서대로 빠짐없이, 겹치지 않게 보냅니다.", requiredMode = Schema.RequiredMode.REQUIRED))
    val formatIds: List<Long?>? = null,
) {
    fun toCommand(): AdminAdChannelFormatOrderCommand = AdminAdChannelFormatOrderCommand(platform = platform, formatIds = formatIds)
}
