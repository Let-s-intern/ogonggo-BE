package com.ogonggo.core.featureflag.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.featureflag.domain.FeatureFlag
import com.ogonggo.core.featureflag.error.FeatureFlagErrorCode
import com.ogonggo.core.featureflag.persistence.FeatureFlagJpaRepository
import org.springframework.stereotype.Component

@Component
class FeatureFlagReader internal constructor(
    private val featureFlagRepository: FeatureFlagJpaRepository,
) {

    /** key 순으로 모든 플래그를 준다. */
    fun readAll(): List<FeatureFlag> = featureFlagRepository.findAllByOrderByFlagKeyAsc()

    fun read(flagKey: String): FeatureFlag =
        featureFlagRepository.findByFlagKey(flagKey)
            ?: throw EntityNotFoundException(FeatureFlagErrorCode.FEATURE_FLAG_NOT_FOUND)
}
