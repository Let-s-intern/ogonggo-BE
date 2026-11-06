package com.ogonggo.adminapi.adposting.presentation.request

import com.ogonggo.adminapi.adposting.business.AdminAdPostingCreateCommand
import com.ogonggo.adminapi.adposting.business.AdminAdPostingOrderCommand
import com.ogonggo.adminapi.adposting.business.AdminAdPostingUpdateCommand
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. 대상 종류는 자리가 정하므로 받지 않는다. */
data class CreateAdminAdPostingRequest(
    @Schema(description = "자리. 등록 뒤 바꾸지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val placement: AdPostingPlacement? = null,
    @Schema(
        description = "채용공고 id. BOOTCAMP_LIST 자리면 부트캠프 id입니다. 등록 뒤 바꾸지 않고, 같은 자리에 같은 대상은 한 번만 둡니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val targetId: Long? = null,
    @Schema(description = "광고주 id", requiredMode = Schema.RequiredMode.REQUIRED)
    val advertiserId: Long? = null,
    @Schema(description = "광고 계약 id. 고른 광고주의 계약만 받습니다. null이면 잇지 않습니다.", nullable = true)
    val contractId: Long? = null,
    @Schema(description = "노출 시작(KST, 포함). null이면 켜는 즉시입니다.", nullable = true)
    val startAt: LocalDateTime? = null,
    @Schema(description = "노출 끝(KST, 미포함). null이면 끌 때까지입니다. 둘 다 있으면 startAt보다 뒤여야 합니다.", nullable = true)
    val endAt: LocalDateTime? = null,
    @Schema(description = "켜기", requiredMode = Schema.RequiredMode.REQUIRED)
    val enabled: Boolean? = null,
) {
    fun toCommand(): AdminAdPostingCreateCommand = AdminAdPostingCreateCommand(
        placement = placement,
        targetId = targetId,
        advertiserId = advertiserId,
        contractId = contractId,
        startAt = startAt,
        endAt = endAt,
        enabled = enabled,
    )
}

/**
 * 보낸 칸만 바꿉니다. 자리와 대상은 바꾸지 않습니다. `contractId`, `startAt`, `endAt`은 null을 보내면 지웁니다.
 * 그 밖의 칸은 null이면 보내지 않은 것과 같습니다. 계약과 기간은 저장된 값과 합친 결과로 검증합니다.
 */
data class UpdateAdminAdPostingRequest(
    val advertiserId: Long? = null,
    @Schema(
        implementation = Long::class,
        nullable = true,
        description = "null을 보내면 계약 연결을 끊습니다. 광고주를 바꾸면 그 광고주의 계약을 함께 보내거나 null로 비웁니다.",
    )
    val contractId: Optional<Long>? = null,
    @Schema(implementation = LocalDateTime::class, nullable = true, description = "null을 보내면 켜는 즉시 노출합니다.")
    val startAt: Optional<LocalDateTime>? = null,
    @Schema(implementation = LocalDateTime::class, nullable = true, description = "null을 보내면 끌 때까지 노출합니다.")
    val endAt: Optional<LocalDateTime>? = null,
    val enabled: Boolean? = null,
) {
    fun toCommand(): AdminAdPostingUpdateCommand = AdminAdPostingUpdateCommand(
        advertiserId = advertiserId,
        contractId = contractId,
        startAt = startAt,
        endAt = endAt,
        enabled = enabled,
    )
}

/** 자리 안 순서를 통째로 바꿉니다. 그 자리의 광고 공고 id를 빠짐없이 새 순서대로 보냅니다. */
data class ReplaceAdminAdPostingOrderRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val placement: AdPostingPlacement? = null,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val adPostingIds: List<Long?>? = null,
) {
    fun toCommand(): AdminAdPostingOrderCommand = AdminAdPostingOrderCommand(placement = placement, adPostingIds = adPostingIds)
}
