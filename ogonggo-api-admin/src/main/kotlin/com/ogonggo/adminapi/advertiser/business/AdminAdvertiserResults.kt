package com.ogonggo.adminapi.advertiser.business

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserSource
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAdCountDto
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import java.time.LocalDateTime

data class AdminAdvertiserResult(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
    val source: AdvertiserSource,
    val companyMemberUserId: Long?,
    val adInquiryId: Long?,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val adCount: AdvertiserAdCountDto,
    val contractTotals: AdminAdvertiserContractTotals,
) {
    companion object {
        internal fun of(
            advertiser: Advertiser,
            adCount: AdvertiserAdCountDto,
            contractTotals: AdminAdvertiserContractTotals,
        ): AdminAdvertiserResult = AdminAdvertiserResult(
            id = advertiser.requiredId(),
            name = advertiser.name,
            type = advertiser.type,
            source = advertiser.source,
            companyMemberUserId = advertiser.companyMemberUserId,
            adInquiryId = advertiser.adInquiryId,
            contactName = advertiser.contactName,
            contactEmail = advertiser.contactEmail,
            contactPhone = advertiser.contactPhone,
            adCount = adCount,
            contractTotals = contractTotals,
        )
    }
}

/** 광고주 하나의 계약 합계다. 금액은 부가세 포함·별도를 가리지 않고 적힌 그대로 더한다. */
data class AdminAdvertiserContractTotals(
    val contractCount: Int,
    val activeContractCount: Int,
    val contractTotalAmount: Long,
    /** 계약끼리 부가세 포함·별도가 섞였는가. 계약이 없으면 false다. */
    val contractVatMixed: Boolean,
) {
    companion object {
        internal fun of(contracts: List<AdvertiserContract>, now: LocalDateTime): AdminAdvertiserContractTotals =
            AdminAdvertiserContractTotals(
                contractCount = contracts.size,
                activeContractCount = contracts.count { it.isActive(now) },
                contractTotalAmount = contracts.sumOf { it.amount },
                contractVatMixed = contracts.map { it.vatIncluded }.distinct().size > 1,
            )
    }
}

internal fun Advertiser.requiredId(): Long = checkNotNull(id) { "광고주 식별자가 없습니다." }
