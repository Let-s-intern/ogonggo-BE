package com.ogonggo.core.banner.implement

import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerSettings
import com.ogonggo.core.banner.persistence.BannerJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BannerManager internal constructor(
    private val bannerRepository: BannerJpaRepository,
) {

    fun change(banner: Banner, settings: BannerSettings) {
        banner.change(settings)
        bannerRepository.save(banner)
    }

    /**
     * 배너를 지우고 그 자리에 남은 배너의 순서를 1부터 다시 매긴다. 이미 지운 배너면 아무것도 바꾸지 않는다.
     * 그 자리를 잠가 읽으므로 호출한 서비스의 트랜잭션 안에서 부른다.
     */
    fun delete(banner: Banner, now: LocalDateTime) {
        if (banner.deletedAt != null) return
        banner.delete(now)
        bannerRepository.saveAndFlush(banner)
        reorder(bannerRepository.findAllInPlacementForUpdate(banner.placement))
    }

    /** 한 자리의 배너 전부를 [banners] 순서대로 1부터 매긴다. 같은 자리의 배너를 빠짐없이 넘긴다. */
    fun reorder(banners: List<Banner>) {
        require(banners.map { it.placement }.distinct().size <= 1) { "한 자리의 배너만 순서를 매깁니다." }
        banners.forEachIndexed { index, banner -> banner.changeDisplayOrder(index + 1) }
        bannerRepository.saveAll(banners)
    }

    /** 지운 광고 계약을 가리키던 배너에서 계약 연결만 끊는다. 배너는 지우지 않는다. */
    fun unlinkContract(contractId: Long) {
        val banners = bannerRepository.findAllByContractId(contractId)
        banners.forEach(Banner::unlinkContract)
        bannerRepository.saveAll(banners)
    }
}
