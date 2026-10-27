package com.ogonggo.adminapi.advertisementinquiry.business

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryPageDto
import java.time.LocalDateTime

data class AdminAdvertisementInquiryPageResult(
    val items: List<AdminAdvertisementInquirySummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: AdvertisementInquiryPageDto): AdminAdvertisementInquiryPageResult =
            AdminAdvertisementInquiryPageResult(
                items = result.advertisementInquiries.map(AdminAdvertisementInquirySummary::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
    }
}

data class AdminAdvertisementInquirySummary(
    val id: Long,
    val inquiryType: AdvertisementInquiryType,
    val campaignCode: String?,
    val status: AdvertisementInquiryStatus,
    val companyName: String,
    val managerName: String,
    val managerTitle: String?,
    val email: String,
    val phoneNumber: String,
    val promotionChannel: AdvertisementInquiryPromotionChannel,
    val hiringJobName: String?,
    val jobPostingUrl: String?,
    val promotionAnswer: String,
    val marketingConsent: Boolean,
    val userId: Long?,
    val memberType: AdvertisementInquiryMemberType,
    val inflowChannel: AdvertisementInquiryInflowChannel,
    val utmSource: String?,
    val utmMedium: String?,
    val utmCampaign: String?,
    val utmContent: String?,
    val referrer: String?,
    val entryHost: String?,
    val landingPath: String?,
    val memo: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(inquiry: AdvertisementInquiry): AdminAdvertisementInquirySummary = AdminAdvertisementInquirySummary(
            id = checkNotNull(inquiry.id) { "광고 문의 식별자가 없습니다." },
            inquiryType = inquiry.inquiryType,
            campaignCode = inquiry.campaignCode,
            status = inquiry.status,
            companyName = inquiry.companyName,
            managerName = inquiry.managerName,
            managerTitle = inquiry.managerTitle,
            email = inquiry.email,
            phoneNumber = inquiry.phoneNumber,
            promotionChannel = inquiry.promotionChannel,
            hiringJobName = inquiry.hiringJobName,
            jobPostingUrl = inquiry.jobPostingUrl,
            promotionAnswer = inquiry.promotionAnswer,
            marketingConsent = inquiry.marketingConsent,
            userId = inquiry.userId,
            memberType = inquiry.memberType,
            inflowChannel = inquiry.inflowChannel,
            utmSource = inquiry.utmSource,
            utmMedium = inquiry.utmMedium,
            utmCampaign = inquiry.utmCampaign,
            utmContent = inquiry.utmContent,
            referrer = inquiry.referrer,
            entryHost = inquiry.entryHost,
            landingPath = inquiry.landingPath,
            memo = inquiry.memo,
            registeredAt = inquiry.createdAt,
        )
    }
}
