package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementReportInsight
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDraftDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractProductJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementReportInsightJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementReportInsightManager internal constructor(
    private val advertisementReportInsightRepository: AdvertisementReportInsightJpaRepository,
    private val advertisementContractProductRepository: AdvertisementContractProductJpaRepository,
) {

    /**
     * 계약 상품의 인사이트를 [drafts]로 통째로 바꾼다. 이전 인사이트는 지운 것으로 한다.
     * [publish]가 참이면 지금 기업 회원에게 보내고, 거짓이면 운영자만 보는 임시 저장이다. 빈 목록이면 모두 걷어낸다.
     * 계약에 붙은 상품이 아니면 404다. 호출하는 쪽이 계약을 잠그고 부른다.
     */
    fun replace(
        contract: AdvertisementContract,
        contractProductId: Long,
        drafts: List<AdvertisementReportInsightDraftDto>,
        publish: Boolean,
        now: LocalDateTime,
    ): List<AdvertisementReportInsightDto> {
        advertisementContractProductRepository.findByIdAndContractIdAndDeletedAtIsNull(contractProductId, checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." })
            ?: throw EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND)
        require(drafts.size <= AdvertisementReportInsight.MAX_COUNT) { "인사이트는 ${AdvertisementReportInsight.MAX_COUNT}개까지 쓸 수 있습니다." }
        advertisementReportInsightRepository.softDeleteAll(contractProductId, now)
        val publishedAt = if (publish) now else null
        return drafts.mapIndexed { index, draft ->
            val saved = advertisementReportInsightRepository.save(
                AdvertisementReportInsight(contractProductId, draft.title.trim(), draft.body.trim(), index, publishedAt),
            )
            AdvertisementReportInsightDto(saved.title, saved.body, saved.published)
        }
    }
}
