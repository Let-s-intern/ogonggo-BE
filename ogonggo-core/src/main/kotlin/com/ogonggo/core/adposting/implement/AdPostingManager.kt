package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.implement.dto.AdPostingUpdateDto
import com.ogonggo.core.adposting.persistence.AdPostingJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdPostingManager internal constructor(
    private val adPostingRepository: AdPostingJpaRepository,
) {

    fun change(adPosting: AdPosting, dto: AdPostingUpdateDto) {
        adPosting.change(
            advertiserId = dto.advertiserId,
            contractId = dto.contractId,
            period = dto.period,
            enabled = dto.enabled,
        )
        adPostingRepository.save(adPosting)
    }

    /** 지우고 그 자리에 남은 광고 공고의 순서를 1부터 다시 매긴다. 이미 지운 광고 공고면 아무것도 바꾸지 않는다. */
    fun delete(adPosting: AdPosting, now: LocalDateTime) {
        if (adPosting.deletedAt != null) {
            return
        }
        adPosting.delete(now)
        adPostingRepository.save(adPosting)
        renumber(adPostingRepository.findAllByPlacementForUpdate(adPosting.placement).filter { it.id != adPosting.id })
    }

    /**
     * 자리 안 순서를 [adPostingIds] 순서로 바꾼다. 그 자리의 광고 공고 식별자를 빠짐없이 한 번씩 받아야 하며,
     * 확인은 부르는 쪽이 한다.
     */
    fun reorder(placement: AdPostingPlacement, adPostingIds: List<Long>) {
        val adPostings = adPostingRepository.findAllByPlacementForUpdate(placement).associateBy { checkNotNull(it.id) }
        require(adPostings.keys == adPostingIds.toSet() && adPostingIds.size == adPostings.size) {
            "자리의 광고 공고 식별자를 빠짐없이 한 번씩 받아야 합니다."
        }
        renumber(adPostingIds.map(adPostings::getValue))
    }

    /** 지운 계약을 잇고 있던 광고 공고(지운 것 포함)의 계약 연결을 끊는다. */
    fun unlinkContract(contractId: Long) {
        val adPostings = adPostingRepository.findAllByContractId(contractId)
        adPostings.forEach(AdPosting::unlinkContract)
        adPostingRepository.saveAll(adPostings)
    }

    private fun renumber(ordered: List<AdPosting>) {
        ordered.forEachIndexed { index, adPosting -> adPosting.moveTo(index + 1) }
        adPostingRepository.saveAll(ordered)
    }
}
