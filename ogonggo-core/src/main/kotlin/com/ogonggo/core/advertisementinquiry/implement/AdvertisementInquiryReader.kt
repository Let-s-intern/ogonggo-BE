package com.ogonggo.core.advertisementinquiry.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.error.AdvertisementInquiryErrorCode
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryPageDto
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryJpaRepository
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
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

    /** 상태·메모를 바꿀 문의를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(inquiryId: Long): AdvertisementInquiry =
        advertisementInquiryRepository.findForUpdate(inquiryId)
            ?: throw EntityNotFoundException(AdvertisementInquiryErrorCode.ADVERTISEMENT_INQUIRY_NOT_FOUND)

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
     * 서비스 시간대(한국 시간)로 오늘 0시부터 들어온 문의 중 반려하지 않은 문의 수다. 문의 유형은 가리지 않는다.
     * 반려로 바꾸면 줄고, 반려를 다른 상태로 되돌리면 다시 는다.
     */
    fun countToday(): Long {
        val today = LocalDate.now(clock)
        return advertisementInquiryRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            AdvertisementInquiryStatus.REJECTED,
            today.atStartOfDay(),
            today.plusDays(1).atStartOfDay(),
        )
    }
}
