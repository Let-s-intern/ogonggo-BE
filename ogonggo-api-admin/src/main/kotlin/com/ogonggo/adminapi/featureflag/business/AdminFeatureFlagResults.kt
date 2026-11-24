package com.ogonggo.adminapi.featureflag.business

import com.ogonggo.core.featureflag.domain.FeatureFlag
import java.time.LocalDateTime

data class AdminFeatureFlagResult(
    val key: String,
    val enabled: Boolean,
    val description: String,
    val updatedAt: LocalDateTime,
    val updatedBy: Long?,
) {
    companion object {
        internal fun from(featureFlag: FeatureFlag): AdminFeatureFlagResult = AdminFeatureFlagResult(
            key = featureFlag.flagKey,
            enabled = featureFlag.enabled,
            description = featureFlag.description,
            updatedAt = featureFlag.updatedAt,
            updatedBy = featureFlag.updatedBy,
        )
    }
}
