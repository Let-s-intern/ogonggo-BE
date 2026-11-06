package com.ogonggo.adminapi.adposting.presentation.response

import com.ogonggo.adminapi.adposting.business.AdminAdPostingResult
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.advertiser.domain.AdvertiserType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdPostingResponse(
    val id: Long,
    val placement: AdPostingPlacement,
    @Schema(description = "자리가 정합니다. BOOTCAMP_LIST만 BOOTCAMP입니다.")
    val targetType: AdPostingTargetType,
    @Schema(description = "채용공고 id 또는 부트캠프 id")
    val targetId: Long,
    @Schema(description = "대상 제목. 대상이 지워졌으면 빈 문자열입니다.")
    val targetTitle: String,
    @Schema(description = "대상 회사. 대상이 지워졌으면 빈 문자열입니다.")
    val targetCompanyName: String,
    @Schema(description = "대상이 지금 공개이고 모집 중인가. 아니면 켜 두어도 사용자에게 나가지 않습니다.")
    val targetAvailable: Boolean,
    @Schema(description = "노출 시작(KST, 포함). null이면 켜는 즉시입니다.")
    val startAt: LocalDateTime?,
    @Schema(description = "노출 끝(KST, 미포함). null이면 끌 때까지입니다.")
    val endAt: LocalDateTime?,
    val enabled: Boolean,
    @Schema(description = "자리 안 순서이자 목록의 칸 번호(1부터)")
    val displayOrder: Int,
    val advertiserId: Long,
    @Schema(description = "광고주 요약. 광고주를 찾을 수 없으면 null입니다.")
    val advertiser: AdminAdPostingAdvertiserResponse?,
    val contractId: Long?,
    @Schema(description = "이어 둔 광고 계약 요약. 없으면 null입니다.")
    val contract: AdminAdPostingContractResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdPostingResult): AdminAdPostingResponse = AdminAdPostingResponse(
            id = result.id,
            placement = result.placement,
            targetType = result.targetType,
            targetId = result.targetId,
            targetTitle = result.targetTitle,
            targetCompanyName = result.targetCompanyName,
            targetAvailable = result.targetAvailable,
            startAt = result.startAt,
            endAt = result.endAt,
            enabled = result.enabled,
            displayOrder = result.displayOrder,
            advertiserId = result.advertiserId,
            advertiser = result.advertiser?.let { AdminAdPostingAdvertiserResponse(it.id, it.name, it.type) },
            contractId = result.contractId,
            contract = result.contract?.let { AdminAdPostingContractResponse(it.id, it.name, it.startAt, it.endAt) },
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 광고 공고 표에 보일 광고주 요약이다. */
data class AdminAdPostingAdvertiserResponse(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)

/** 광고 공고 표와 기간 밖 경고에 쓸 광고 계약 요약이다. */
data class AdminAdPostingContractResponse(
    val id: Long,
    @Schema(description = "계약명. 없으면 null입니다.")
    val name: String?,
    @Schema(description = "광고 시작(KST, 포함)")
    val startAt: LocalDateTime,
    @Schema(description = "광고 끝(KST, 미포함)")
    val endAt: LocalDateTime,
)

/** 지운 광고 공고의 식별자다. */
data class AdminAdPostingIdResponse(
    val id: Long,
)
