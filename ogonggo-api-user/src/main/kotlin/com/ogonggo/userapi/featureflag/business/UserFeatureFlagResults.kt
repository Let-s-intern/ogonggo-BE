package com.ogonggo.userapi.featureflag.business

import com.ogonggo.core.featureflag.domain.FeatureFlag

/** 설명과 변경자는 사용자에게 내리지 않으므로 key와 켜짐 여부만 둔다. */
data class UserFeatureFlagResult(
    val key: String,
    val enabled: Boolean,
) {
    companion object {
        internal fun from(featureFlag: FeatureFlag): UserFeatureFlagResult = UserFeatureFlagResult(
            key = featureFlag.flagKey,
            enabled = featureFlag.enabled,
        )
    }
}
