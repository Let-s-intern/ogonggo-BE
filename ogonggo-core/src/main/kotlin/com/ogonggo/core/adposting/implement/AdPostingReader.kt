package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.error.AdPostingErrorCode
import com.ogonggo.core.adposting.persistence.AdPostingJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component

@Component
class AdPostingReader internal constructor(
    private val adPostingRepository: AdPostingJpaRepository,
) {

    fun read(adPostingId: Long): AdPosting =
        adPostingRepository.findByIdAndDeletedAtIsNull(adPostingId)
            ?: throw EntityNotFoundException(AdPostingErrorCode.AD_POSTING_NOT_FOUND)

    /** 수정할 광고 공고를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(adPostingId: Long): AdPosting =
        adPostingRepository.findForUpdate(adPostingId)
            ?: throw EntityNotFoundException(AdPostingErrorCode.AD_POSTING_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 광고 공고도 잠가 찾는다. */
    fun readForDelete(adPostingId: Long): AdPosting =
        adPostingRepository.findIncludingDeletedForUpdate(adPostingId)
            ?: throw EntityNotFoundException(AdPostingErrorCode.AD_POSTING_NOT_FOUND)

    /** 자리 하나의 광고 공고 전부다. 꺼진 것·기간이 지난 것도 싣고 자리 안 순서대로 준다. */
    fun readAllByPlacement(placement: AdPostingPlacement): List<AdPosting> =
        adPostingRepository.findAllByPlacementAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(placement)

    /** 순서를 바꾸려고 자리 하나의 광고 공고를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readAllByPlacementForUpdate(placement: AdPostingPlacement): List<AdPosting> =
        adPostingRepository.findAllByPlacementForUpdate(placement)

    /**
     * 광고주 하나의 광고 공고다. [placement]를 주면 그 자리 안에서만 찾는다.
     * 자리 선언 순서, 자리 안 순서대로 준다.
     */
    fun readAllByAdvertiser(advertiserId: Long, placement: AdPostingPlacement?): List<AdPosting> =
        adPostingRepository.findAllByAdvertiserIdAndDeletedAtIsNull(advertiserId)
            .filter { placement == null || it.placement == placement }
            .sortedWith(compareBy<AdPosting>({ it.placement.ordinal }, { it.displayOrder }, { it.id }))

    /** 같은 자리에 같은 대상이 이미 있는가. 지운 광고 공고는 보지 않는다. */
    fun existsInPlacement(placement: AdPostingPlacement, targetId: Long): Boolean =
        adPostingRepository.existsByPlacementAndTargetIdAndDeletedAtIsNull(placement, targetId)
}
