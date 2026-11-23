package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementReportInsightJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertisementReportInsightReader internal constructor(
    private val advertisementReportInsightRepository: AdvertisementReportInsightJpaRepository,
) {

    /** 계약 상품의 인사이트를 보이는 순서로 준다. [includeDrafts]가 거짓이면 기업 회원에게 보낸 것만 준다. */
    fun readByContractProduct(contractProductId: Long, includeDrafts: Boolean): List<AdvertisementReportInsightDto> =
        advertisementReportInsightRepository.findAllByContractProductIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(contractProductId)
            .filter { includeDrafts || it.published }
            .map { AdvertisementReportInsightDto(it.title, it.body, it.published) }
}
