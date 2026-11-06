package com.ogonggo.core.banner.implement

import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.error.BannerErrorCode
import com.ogonggo.core.banner.persistence.BannerJpaRepository
import com.ogonggo.core.banner.persistence.BannerQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BannerReader internal constructor(
    private val bannerRepository: BannerJpaRepository,
    private val bannerQueryRepository: BannerQueryRepository,
) {

    fun read(bannerId: Long): Banner =
        bannerRepository.findByIdAndDeletedAtIsNull(bannerId)
            ?: throw EntityNotFoundException(BannerErrorCode.BANNER_NOT_FOUND)

    /** 수정할 배너를 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(bannerId: Long): Banner =
        bannerRepository.findForUpdate(bannerId)
            ?: throw EntityNotFoundException(BannerErrorCode.BANNER_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 배너도 잠가 찾는다. */
    fun readForDelete(bannerId: Long): Banner =
        bannerRepository.findIncludingDeletedForUpdate(bannerId)
            ?: throw EntityNotFoundException(BannerErrorCode.BANNER_NOT_FOUND)

    /** 자리 하나의 배너 전부(꺼진 것·기간이 지난 것 포함)를 순서대로 읽는다. */
    fun readAllInPlacement(placement: BannerPlacement): List<Banner> =
        bannerRepository.findAllByPlacementAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(placement)

    /** 자리 하나의 배너를 잠가 순서대로 읽는다. 순서를 바꾸는 쓰기는 이것으로 시작한다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readAllInPlacementForUpdate(placement: BannerPlacement): List<Banner> =
        bannerRepository.findAllInPlacementForUpdate(placement)

    /** 광고주 하나의 배너를 자리 선언 순서, 자리 안 순서대로 읽는다. */
    fun readAllByAdvertiser(advertiserId: Long): List<Banner> =
        bannerRepository.findAllByAdvertiserIdAndDeletedAtIsNull(advertiserId).sortedWith(DISPLAY_ORDER)

    /** 광고주 여럿의 배너다. 순서는 정하지 않는다. */
    fun readAllByAdvertisers(advertiserIds: Collection<Long>): List<Banner> =
        if (advertiserIds.isEmpty()) emptyList() else bannerRepository.findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds)

    /** 광고 계약 여럿에 이어 둔 배너다. 순서는 정하지 않는다. */
    fun readAllByContracts(contractIds: Collection<Long>): List<Banner> =
        if (contractIds.isEmpty()) emptyList() else bannerRepository.findAllByContractIdInAndDeletedAtIsNull(contractIds)

    /** [placement]에서 [now]에 노출 중인 배너를 순서대로 읽는다. */
    fun readLive(placement: BannerPlacement, now: LocalDateTime): List<Banner> =
        bannerQueryRepository.findLive(placement, now)

    companion object {
        private val DISPLAY_ORDER = compareBy<Banner>({ it.placement.ordinal }, { it.displayOrder }, { it.id })
    }
}
