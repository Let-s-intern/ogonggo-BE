package com.ogonggo.adminapi.advertisementinquiry.business

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    /** 어느 상태로든 바꾼다. 바뀐 문의 전체를 돌려준다. */
    @Transactional
    fun changeStatus(inquiryId: Long, status: AdvertisementInquiryStatus): AdminAdvertisementInquirySummary {
        val inquiry = advertisementInquiryReader.readForUpdate(inquiryId)
        inquiry.changeStatus(status)
        return AdminAdvertisementInquirySummary.from(inquiry)
    }

    /** 메모를 통째로 바꾼다. null이나 공백이면 지운다. 바뀐 문의 전체를 돌려준다. */
    @Transactional
    fun changeMemo(inquiryId: Long, memo: String?): AdminAdvertisementInquirySummary {
        val inquiry = advertisementInquiryReader.readForUpdate(inquiryId)
        inquiry.changeMemo(memo)
        return AdminAdvertisementInquirySummary.from(inquiry)
    }
}
