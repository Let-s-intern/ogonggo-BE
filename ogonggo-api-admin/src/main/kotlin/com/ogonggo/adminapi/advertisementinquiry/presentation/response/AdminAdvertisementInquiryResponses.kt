package com.ogonggo.adminapi.advertisementinquiry.presentation.response

import com.ogonggo.adminapi.advertisementinquiry.business.AdminAdvertisementInquirySummary
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdvertisementInquiryResponse(
    val id: Long,
    val inquiryType: AdvertisementInquiryType,
    @Schema(description = "같은 문의 유형 안의 이벤트. 서버가 진입 페이지로 정하며 없을 수 있습니다.", example = "launch_free_promo")
    val campaignCode: String?,
    val status: AdvertisementInquiryStatus,
    val companyName: String,
    val managerName: String,
    @Schema(description = "담당자 직함. 이 필드가 생기기 전 폼에서 온 문의는 null입니다.")
    val managerTitle: String?,
    val email: String,
    val phoneNumber: String,
    val promotionChannel: AdvertisementInquiryPromotionChannel,
    @Schema(description = "채용 직무명. 이 필드가 생기기 전 폼에서 온 문의는 null입니다.")
    val hiringJobName: String?,
    @Schema(description = "채용공고 링크. 이 필드가 생기기 전 폼에서 온 문의는 null입니다.")
    val jobPostingUrl: String?,
    @Schema(description = "현재 사용 중인 채용 홍보 채널과 어려운 점")
    val promotionAnswer: String,
    @Schema(description = "마케팅 정보 수신 동의")
    val marketingConsent: Boolean,
    @Schema(description = "문의한 회원 식별자. 비회원이면 null입니다.")
    val userId: Long?,
    @Schema(description = "문의 시점의 회원 구분. 서버가 토큰으로 정했습니다.")
    val memberType: AdvertisementInquiryMemberType,
    @Schema(
        description = "저장할 때 판정한 유입 경로. UTM_OTHER는 utmSource 원문을, EXTERNAL_SITE는 referrer 호스트를 함께 보이면 됩니다.",
    )
    val inflowChannel: AdvertisementInquiryInflowChannel,
    val utmSource: String?,
    val utmMedium: String?,
    val utmCampaign: String?,
    val utmContent: String?,
    @Schema(description = "진입 시 document.referrer 원문")
    val referrer: String?,
    @Schema(description = "처음 들어온 호스트(리다이렉트 전)")
    val entryHost: String?,
    @Schema(description = "진입 URL의 경로와 쿼리")
    val landingPath: String?,
    @Schema(description = "영업 담당자 메모")
    val memo: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdvertisementInquirySummary): AdminAdvertisementInquiryResponse =
            AdminAdvertisementInquiryResponse(
                id = result.id,
                inquiryType = result.inquiryType,
                campaignCode = result.campaignCode,
                status = result.status,
                companyName = result.companyName,
                managerName = result.managerName,
                managerTitle = result.managerTitle,
                email = result.email,
                phoneNumber = result.phoneNumber,
                promotionChannel = result.promotionChannel,
                hiringJobName = result.hiringJobName,
                jobPostingUrl = result.jobPostingUrl,
                promotionAnswer = result.promotionAnswer,
                marketingConsent = result.marketingConsent,
                userId = result.userId,
                memberType = result.memberType,
                inflowChannel = result.inflowChannel,
                utmSource = result.utmSource,
                utmMedium = result.utmMedium,
                utmCampaign = result.utmCampaign,
                utmContent = result.utmContent,
                referrer = result.referrer,
                entryHost = result.entryHost,
                landingPath = result.landingPath,
                memo = result.memo,
                registeredAt = result.registeredAt,
            )
    }
}

data class AdminAdvertisementInquiryTodayCountResponse(
    @Schema(description = "한국 시간 오늘 0시부터 들어온 광고 문의 수. 문의 유형과 처리 상태를 가리지 않습니다.")
    val count: Long,
)
