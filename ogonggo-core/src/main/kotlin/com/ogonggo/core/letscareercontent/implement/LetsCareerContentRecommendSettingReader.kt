package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights
import com.ogonggo.core.letscareercontent.persistence.LetsCareerContentRecommendSettingJpaRepository
import org.springframework.stereotype.Component

@Component
class LetsCareerContentRecommendSettingReader internal constructor(
    private val settingRepository: LetsCareerContentRecommendSettingJpaRepository,
) {

    /** 저장한 점수다. 운영자가 한 번도 바꾸지 않았으면 기본값이다. */
    fun readWeights(): LetsCareerContentRecommendWeights =
        settingRepository.findFirstByOrderByIdAsc()?.weights() ?: LetsCareerContentRecommendWeights.DEFAULT
}
