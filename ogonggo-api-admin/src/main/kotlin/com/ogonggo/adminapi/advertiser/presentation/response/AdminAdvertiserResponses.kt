package com.ogonggo.adminapi.advertiser.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserResult
import com.ogonggo.core.advertiser.domain.AdvertiserSource
import com.ogonggo.core.advertiser.domain.AdvertiserType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 값이 없는 담당자·회원·문의 칸은 응답에서 뺍니다(프런트 계약의 선택 칸). */
data class AdminAdvertiserResponse(
    val id: Long,
    @Schema(description = "기업 이름. 회원 광고주는 기관명입니다.")
    val name: String,
    val type: AdvertiserType,
    val source: AdvertiserSource,
    @Schema(description = "비즈니스 회원의 사용자 식별자. 회원 광고주에만 있습니다.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val companyMemberUserId: Long?,
    @Schema(description = "이 기업이 남긴 가장 최근 광고 문의 식별자. 광고 문의로 들어왔을 때만 있습니다.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val adInquiryId: Long?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val contactName: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val contactEmail: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val contactPhone: String?,
    @Schema(description = "이 광고주의 배너 수(꺼진 것·기간이 끝난 것 포함)")
    val bannerCount: Int,
    @Schema(description = "지금 노출 중인 배너 수")
    val activeBannerCount: Int,
    @Schema(description = "이 광고주의 광고 공고 수(모든 자리, 꺼진 것 포함)")
    val adPostingCount: Int,
    @Schema(description = "지금 노출 중이고 대상이 공개·모집 중인 광고 공고 수")
    val activeAdPostingCount: Int,
    @Schema(description = "배너·광고 공고 노출 끝 가운데 가장 늦은 것(KST). 끝이 정해진 광고가 없으면 null입니다.")
    val latestEndAt: LocalDateTime?,
    val contractCount: Int,
    @Schema(description = "지금 기간 안(시작 포함, 끝 미포함)인 계약 수")
    val activeContractCount: Int,
    @Schema(description = "모든 계약 금액의 합(원). 부가세 포함·별도를 가리지 않고 적힌 금액 그대로 더합니다.")
    val contractTotalAmount: Long,
    @Schema(description = "계약끼리 부가세 포함·별도가 섞였는가. 계약이 없으면 false입니다.")
    val contractVatMixed: Boolean,
) {
    companion object {
        internal fun from(result: AdminAdvertiserResult): AdminAdvertiserResponse = AdminAdvertiserResponse(
            id = result.id,
            name = result.name,
            type = result.type,
            source = result.source,
            companyMemberUserId = result.companyMemberUserId,
            adInquiryId = result.adInquiryId,
            contactName = result.contactName,
            contactEmail = result.contactEmail,
            contactPhone = result.contactPhone,
            bannerCount = result.adCount.bannerCount,
            activeBannerCount = result.adCount.activeBannerCount,
            adPostingCount = result.adCount.adPostingCount,
            activeAdPostingCount = result.adCount.activeAdPostingCount,
            latestEndAt = result.adCount.latestEndAt,
            contractCount = result.contractTotals.contractCount,
            activeContractCount = result.contractTotals.activeContractCount,
            contractTotalAmount = result.contractTotals.contractTotalAmount,
            contractVatMixed = result.contractTotals.contractVatMixed,
        )
    }
}
