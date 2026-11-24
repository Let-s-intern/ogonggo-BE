package com.ogonggo.core.featureflag.persistence

import com.ogonggo.core.featureflag.domain.FeatureFlag
import org.springframework.data.jpa.repository.JpaRepository

internal interface FeatureFlagJpaRepository : JpaRepository<FeatureFlag, Long> {
    fun findAllByOrderByFlagKeyAsc(): List<FeatureFlag>

    fun findByFlagKey(flagKey: String): FeatureFlag?
}
