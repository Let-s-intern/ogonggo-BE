package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendSetting
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights
import com.ogonggo.core.letscareercontent.persistence.LetsCareerContentRecommendSettingJpaRepository
import org.springframework.stereotype.Component

@Component
class LetsCareerContentRecommendSettingManager internal constructor(
    private val settingRepository: LetsCareerContentRecommendSettingJpaRepository,
) {

    /** 점수를 통째로 바꾼다. 행이 없으면 만든다. */
    fun replace(weights: LetsCareerContentRecommendWeights) {
        val setting = settingRepository.findFirstByOrderByIdAsc()
        if (setting == null) {
            settingRepository.save(LetsCareerContentRecommendSetting(weights))
            return
        }
        setting.replace(weights)
    }
}
