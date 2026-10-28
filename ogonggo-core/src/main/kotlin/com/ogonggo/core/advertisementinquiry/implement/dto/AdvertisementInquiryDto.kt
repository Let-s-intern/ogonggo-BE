package com.ogonggo.core.advertisementinquiry.implement.dto

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflow
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType

data class AdvertisementInquiryAppendDto(
    val inquiryType: AdvertisementInquiryType,
    val campaignCode: String?,
    val companyName: String,
    val companyNameSource: AdvertisementInquiryCompanyNameSource,
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
    val inflow: AdvertisementInquiryInflow,
)

data class AdvertisementInquiryPageDto(
    val advertisementInquiries: List<AdvertisementInquiry>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
