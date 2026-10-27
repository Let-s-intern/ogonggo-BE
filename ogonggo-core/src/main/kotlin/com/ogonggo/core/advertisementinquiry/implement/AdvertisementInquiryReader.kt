package com.ogonggo.core.advertisementinquiry.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryPageDto
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryJpaRepository
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryQueryRepository
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
class AdvertisementInquiryReader internal constructor(
    private val advertisementInquiryRepository: AdvertisementInquiryJpaRepository,
    private val advertisementInquiryQueryRepository: AdvertisementInquiryQueryRepository,
    private val clock: Clock,
) {

    /** 최근에 들어온 문의부터 준다. */
    fun readPage(condition: AdvertisementInquirySearchCondition, page: Int, size: Int): AdvertisementInquiryPageDto {
        validatePageRequest(page, size)
        val result = advertisementInquiryQueryRepository.findPage(condition, PageRequest.of(page, size))
        return AdvertisementInquiryPageDto(
            advertisementInquiries = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /**
     * 서비스 시간대(한국 시간)로 오늘 0시부터 들어온 문의 수다. 문의 유형과 처리 상태를 가리지 않는다.
     * 처리 상태를 바꿔도 줄지 않는다.
     */
    fun countToday(): Long {
        val today = LocalDate.now(clock)
        return advertisementInquiryRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            today.atStartOfDay(),
            today.plusDays(1).atStartOfDay(),
        )
    }
}
