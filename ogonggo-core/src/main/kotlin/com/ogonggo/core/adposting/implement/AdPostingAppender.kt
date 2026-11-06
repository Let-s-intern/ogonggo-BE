package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.implement.dto.AdPostingAppendDto
import com.ogonggo.core.adposting.persistence.AdPostingJpaRepository
import org.springframework.stereotype.Component

@Component
class AdPostingAppender internal constructor(
    private val adPostingRepository: AdPostingJpaRepository,
) {

    /** 그 자리의 맨 뒤에 붙인다. 자리의 광고 공고를 잠가 순서를 정하므로 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun append(dto: AdPostingAppendDto): AdPosting {
        val lastOrder = adPostingRepository.findAllByPlacementForUpdate(dto.placement).maxOfOrNull { it.displayOrder } ?: 0
        return adPostingRepository.save(
            AdPosting(
                placement = dto.placement,
                targetId = dto.targetId,
                advertiserId = dto.advertiserId,
                contractId = dto.contractId,
                period = dto.period,
                enabled = dto.enabled,
                displayOrder = lastOrder + 1,
            ),
        )
    }
}
