package com.ogonggo.core.letscareercontent.persistence

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendSetting
import org.springframework.data.jpa.repository.JpaRepository

internal interface LetsCareerContentRecommendSettingJpaRepository : JpaRepository<LetsCareerContentRecommendSetting, Long> {
    fun findFirstByOrderByIdAsc(): LetsCareerContentRecommendSetting?
}
