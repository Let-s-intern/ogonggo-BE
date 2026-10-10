package com.ogonggo.core.letscareercontent.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LetsCareerContentRecommendWeightsTest {

    @Test
    fun `음수 점수나 0개인 갈래별 최대 개수는 받지 않는다`() {
        assertThrows<IllegalArgumentException> { LetsCareerContentRecommendWeights.DEFAULT.copy(programBonus = -1) }
        assertThrows<IllegalArgumentException> { LetsCareerContentRecommendWeights.DEFAULT.copy(maxPerGroup = 0) }
    }
}
