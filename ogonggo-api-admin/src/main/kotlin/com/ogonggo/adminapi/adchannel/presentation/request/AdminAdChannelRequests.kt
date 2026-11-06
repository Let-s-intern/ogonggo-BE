package com.ogonggo.adminapi.adchannel.presentation.request

import com.ogonggo.adminapi.adchannel.business.AdminAdChannelCreateCommand
import com.ogonggo.adminapi.adchannel.business.AdminAdChannelUpdateCommand
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminAdChannelRequest(
    @Schema(description = "플랫폼. 등록한 뒤에는 바꾸지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val platform: AdChannelPlatform? = null,
    @Schema(description = "표시 이름. 인스타그램은 계정 표시 이름, 오픈채팅은 방 이름. 1~50자", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String? = null,
    @Schema(
        description = "인스타그램 계정 이름. 인스타그램은 필수이고 영문·숫자·`.`·`_` 1~30자, 대소문자를 무시하고 겹치지 않습니다. " +
            "앞의 `@`는 떼고 저장합니다. 오픈채팅은 보내도 null로 저장합니다.",
        nullable = true,
    )
    val handle: String? = null,
    @Schema(description = "인스타그램 프로필 주소, 오픈채팅방 참여 주소. http(s) 주소", requiredMode = Schema.RequiredMode.REQUIRED)
    val url: String? = null,
    @Schema(description = "프로필 이미지. https 주소 또는 웹 사이트 경로(/community/...)", nullable = true)
    val profileImageUrl: String? = null,
    @Schema(description = "오픈채팅 참여 인원, 인스타그램 팔로워 수. 0 이상 정수, 안내용", nullable = true)
    val memberCount: Long? = null,
    @Schema(description = "끄면 새 채널 광고에서 고를 수 없습니다. 보내지 않으면 켭니다.")
    val enabled: Boolean? = null,
    @Schema(description = "메모. 500자 이하", nullable = true)
    val memo: String? = null,
) {
    fun toCommand(): AdminAdChannelCreateCommand = AdminAdChannelCreateCommand(
        platform = platform,
        name = name,
        handle = handle,
        url = url,
        profileImageUrl = profileImageUrl,
        memberCount = memberCount,
        enabled = enabled,
        memo = memo,
    )
}

/**
 * 보낸 칸만 바꿉니다. `profileImageUrl`, `memberCount`, `memo`는 null을 보내면 지웁니다. 그 밖의 칸은 null이면 보내지 않은 것과
 * 같습니다. `platform`을 저장된 값과 다르게 보내면 400입니다.
 */
data class UpdateAdminAdChannelRequest(
    val platform: AdChannelPlatform? = null,
    val name: String? = null,
    val handle: String? = null,
    val url: String? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 프로필 이미지를 지웁니다.")
    val profileImageUrl: Optional<String>? = null,
    @Schema(implementation = Long::class, nullable = true, description = "null을 보내면 인원 수를 지웁니다.")
    val memberCount: Optional<Long>? = null,
    val enabled: Boolean? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 메모를 지웁니다.")
    val memo: Optional<String>? = null,
) {
    fun toCommand(): AdminAdChannelUpdateCommand = AdminAdChannelUpdateCommand(
        platform = platform,
        name = name,
        handle = handle,
        url = url,
        profileImageUrl = profileImageUrl,
        memberCount = memberCount,
        enabled = enabled,
        memo = memo,
    )
}
