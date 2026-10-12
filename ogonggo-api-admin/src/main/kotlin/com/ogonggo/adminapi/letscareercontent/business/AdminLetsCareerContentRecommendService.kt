package com.ogonggo.adminapi.letscareercontent.business

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights
import com.ogonggo.core.letscareercontent.implement.LetsCareerContentRecommendSettingManager
import com.ogonggo.core.letscareercontent.implement.LetsCareerContentRecommendSettingReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 운영자가 공고별 렛츠커리어 콘텐츠 추천의 점수를 보고 바꾼다. 바꾼 값은 다음 추천 요청부터 쓴다. */
@Service
class AdminLetsCareerContentRecommendService(
    private val settingReader: LetsCareerContentRecommendSettingReader,
    private val settingManager: LetsCareerContentRecommendSettingManager,
) {

    fun getWeights(): LetsCareerContentRecommendWeights = settingReader.readWeights()

    @Transactional
    fun replaceWeights(weights: LetsCareerContentRecommendWeights) {
        settingManager.replace(weights)
    }
}
