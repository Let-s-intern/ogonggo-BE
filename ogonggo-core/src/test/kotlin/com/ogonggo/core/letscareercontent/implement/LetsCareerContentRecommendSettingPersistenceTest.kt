package com.ogonggo.core.letscareercontent.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(LetsCareerContentRecommendSettingReader::class, LetsCareerContentRecommendSettingManager::class)
internal class LetsCareerContentRecommendSettingPersistenceTest @Autowired constructor(
    private val settingReader: LetsCareerContentRecommendSettingReader,
    private val settingManager: LetsCareerContentRecommendSettingManager,
) {

    @Test
    fun `바꾼 적이 없으면 기본값이고 바꾸면 마지막에 바꾼 값이다`() {
        assertEquals(LetsCareerContentRecommendWeights.DEFAULT, settingReader.readWeights())

        settingManager.replace(LetsCareerContentRecommendWeights.DEFAULT.copy(programBonus = 4))
        settingManager.replace(LetsCareerContentRecommendWeights.DEFAULT.copy(programBonus = 5, maxPerGroup = 3))

        assertEquals(LetsCareerContentRecommendWeights.DEFAULT.copy(programBonus = 5, maxPerGroup = 3), settingReader.readWeights())
    }
}
