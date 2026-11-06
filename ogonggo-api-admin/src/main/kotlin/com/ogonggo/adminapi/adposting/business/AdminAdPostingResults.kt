package com.ogonggo.adminapi.adposting.business

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.dto.AdPostingTargetDto
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import java.time.LocalDateTime

data class AdminAdPostingResult(
    val id: Long,
    val placement: AdPostingPlacement,
    val targetType: AdPostingTargetType,
    val targetId: Long,
    val targetTitle: String,
    val targetCompanyName: String,
    val targetAvailable: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean,
    val displayOrder: Int,
    val advertiserId: Long,
    val advertiser: AdminAdPostingAdvertiserResult?,
    val contractId: Long?,
    val contract: AdminAdPostingContractResult?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(
            adPosting: AdPosting,
            target: AdPostingTargetDto,
            advertiser: Advertiser?,
            contract: AdvertiserContract?,
        ): AdminAdPostingResult = AdminAdPostingResult(
            id = adPosting.requiredId(),
            placement = adPosting.placement,
            targetType = adPosting.targetType,
            targetId = adPosting.targetId,
            targetTitle = target.title,
            targetCompanyName = target.companyName,
            targetAvailable = target.available,
            startAt = adPosting.startAt,
            endAt = adPosting.endAt,
            enabled = adPosting.enabled,
            displayOrder = adPosting.displayOrder,
            advertiserId = adPosting.advertiserId,
            advertiser = advertiser?.let {
                AdminAdPostingAdvertiserResult(id = checkNotNull(it.id), name = it.name, type = it.type)
            },
            contractId = adPosting.contractId,
            contract = contract?.let {
                AdminAdPostingContractResult(id = checkNotNull(it.id), name = it.name, startAt = it.startAt, endAt = it.endAt)
            },
            createdAt = adPosting.createdAt,
            updatedAt = adPosting.updatedAt,
        )
    }
}

data class AdminAdPostingAdvertiserResult(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)

data class AdminAdPostingContractResult(
    val id: Long,
    val name: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)

internal fun AdPosting.requiredId(): Long = checkNotNull(id) { "광고 공고 식별자가 없습니다." }
