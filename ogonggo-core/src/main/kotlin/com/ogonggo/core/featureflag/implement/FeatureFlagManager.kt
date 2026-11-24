package com.ogonggo.core.featureflag.implement

import com.ogonggo.core.featureflag.domain.FeatureFlag
import com.ogonggo.core.featureflag.persistence.FeatureFlagJpaRepository
import org.springframework.stereotype.Component

@Component
class FeatureFlagManager internal constructor(
    private val featureFlagRepository: FeatureFlagJpaRepository,
) {

    fun changeEnabled(featureFlag: FeatureFlag, enabled: Boolean, updatedBy: Long) {
        featureFlag.changeEnabled(enabled, updatedBy)
        featureFlagRepository.save(featureFlag)
    }
}
