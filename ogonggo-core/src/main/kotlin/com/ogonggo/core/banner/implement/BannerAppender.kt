package com.ogonggo.core.banner.implement

import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.implement.dto.BannerAppendDto
import com.ogonggo.core.banner.persistence.BannerJpaRepository
import org.springframework.stereotype.Component

@Component
class BannerAppender internal constructor(
    private val bannerRepository: BannerJpaRepository,
) {

    /** 그 자리 맨 뒤(자리 안 개수 + 1)에 붙인다. 그 자리를 잠가 세므로 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun append(dto: BannerAppendDto): Banner {
        val count = bannerRepository.findAllInPlacementForUpdate(dto.placement).size
        return bannerRepository.save(Banner(placement = dto.placement, settings = dto.settings, displayOrder = count + 1))
    }
}
