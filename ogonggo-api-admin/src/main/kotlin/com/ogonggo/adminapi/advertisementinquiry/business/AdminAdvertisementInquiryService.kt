package com.ogonggo.adminapi.advertisementinquiry.business

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryReader
import org.springframework.stereotype.Service

@Service
class AdminAdvertisementInquiryService(
    private val advertisementInquiryReader: AdvertisementInquiryReader,
) {

    fun getAdvertisementInquiries(
        condition: AdvertisementInquirySearchCondition,
        page: Int,
        size: Int,
    ): AdminAdvertisementInquiryPageResult =
        AdminAdvertisementInquiryPageResult.from(advertisementInquiryReader.readPage(condition, page, size))

    /** 메뉴 숫자용. 한국 시간 오늘 0시부터 들어온 문의 수이며 문의 유형과 처리 상태를 가리지 않는다. */
    fun getTodayCount(): Long = advertisementInquiryReader.countToday()
}
