package com.ogonggo.core.advertisementinquiry.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryAppendDto
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertisementInquiryAppender internal constructor(
    private val advertisementInquiryRepository: AdvertisementInquiryJpaRepository,
) {

    /** 저장한 문의의 식별자를 돌려준다. */
    fun append(dto: AdvertisementInquiryAppendDto): Long {
        val saved = advertisementInquiryRepository.save(
            AdvertisementInquiry(
                inquiryType = dto.inquiryType,
                campaignCode = dto.campaignCode,
                companyName = dto.companyName,
                managerName = dto.managerName,
                managerTitle = dto.managerTitle,
                email = dto.email,
                phoneNumber = dto.phoneNumber,
                promotionChannel = dto.promotionChannel,
                hiringJobName = dto.hiringJobName,
                jobPostingUrl = dto.jobPostingUrl,
                promotionAnswer = dto.promotionAnswer,
                marketingConsent = dto.marketingConsent,
                userId = dto.userId,
                memberType = dto.memberType,
                inflow = dto.inflow,
            ),
        )
        return checkNotNull(saved.id) { "광고 문의 식별자가 없습니다." }
    }
}
