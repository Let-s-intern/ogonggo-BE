package com.ogonggo.adminapi.channelad.presentation.request

import com.ogonggo.adminapi.channelad.business.AdminChannelAdCreateCommand
import com.ogonggo.adminapi.channelad.business.AdminChannelAdUpdateCommand
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminChannelAdRequest(
    @Schema(description = "광고주", requiredMode = Schema.RequiredMode.REQUIRED)
    val advertiserId: Long? = null,
    @Schema(description = "이을 광고 계약. 같은 광고주의 계약만 이을 수 있습니다.", nullable = true)
    val contractId: Long? = null,
    @Schema(description = "채널. 사용 안 함인 채널은 고를 수 없습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val channelId: Long? = null,
    @Schema(
        description = "게재 유형. 채널과 같은 플랫폼이고, 유형의 channelIds가 비어 있지 않으면 그 안의 채널이어야 합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val formatId: Long? = null,
    @Schema(description = "게시 예정일(KST). 게시 뒤에는 실제 게시일로 고칩니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val postDate: LocalDate? = null,
    @Schema(description = "노출 끝 날(KST, 그날 포함). 기간이 있을 때만 보내며 postDate와 같거나 뒤입니다.", nullable = true)
    val endDate: LocalDate? = null,
    @Schema(description = "진행 상태. 보내지 않으면 SCHEDULED입니다.")
    val status: ChannelAdStatus? = null,
    @Schema(description = "게시 확인 주소. http(s) 주소", nullable = true)
    val postUrl: String? = null,
    @Schema(description = "소재·문구 메모. 1000자 이하", nullable = true)
    val creativeMemo: String? = null,
    @Schema(description = "담당자 메모. 1000자 이하", nullable = true)
    val adminMemo: String? = null,
) {
    fun toCommand(): AdminChannelAdCreateCommand = AdminChannelAdCreateCommand(
        advertiserId = advertiserId,
        contractId = contractId,
        channelId = channelId,
        formatId = formatId,
        postDate = postDate,
        endDate = endDate,
        status = status,
        postUrl = postUrl,
        creativeMemo = creativeMemo,
        adminMemo = adminMemo,
    )
}

/**
 * 보낸 칸만 바꿉니다. `contractId`, `endDate`, `postUrl`, `creativeMemo`, `adminMemo`는 null을 보내면 지웁니다. 그 밖의 칸은
 * null이면 보내지 않은 것과 같습니다. 광고주만 바꾸고 `contractId`를 보내지 않으면 계약을 끊습니다.
 */
data class UpdateAdminChannelAdRequest(
    val advertiserId: Long? = null,
    @Schema(implementation = Long::class, nullable = true, description = "null을 보내면 계약 연결을 끊습니다.")
    val contractId: Optional<Long>? = null,
    val channelId: Long? = null,
    val formatId: Long? = null,
    val postDate: LocalDate? = null,
    @Schema(implementation = LocalDate::class, nullable = true, description = "null을 보내면 하루짜리 게시가 됩니다.")
    val endDate: Optional<LocalDate>? = null,
    val status: ChannelAdStatus? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 게시 확인 주소를 지웁니다.")
    val postUrl: Optional<String>? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 소재 메모를 지웁니다.")
    val creativeMemo: Optional<String>? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 담당자 메모를 지웁니다.")
    val adminMemo: Optional<String>? = null,
) {
    fun toCommand(): AdminChannelAdUpdateCommand = AdminChannelAdUpdateCommand(
        advertiserId = advertiserId,
        contractId = contractId,
        channelId = channelId,
        formatId = formatId,
        postDate = postDate,
        endDate = endDate,
        status = status,
        postUrl = postUrl,
        creativeMemo = creativeMemo,
        adminMemo = adminMemo,
    )
}
