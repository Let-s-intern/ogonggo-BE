package com.ogonggo.userapi.featureflag.business

import com.ogonggo.userapi.featureflag.implement.UserFeatureFlagCache
import org.springframework.stereotype.Service

@Service
class UserFeatureFlagService(
    private val userFeatureFlagCache: UserFeatureFlagCache,
) {

    fun getFeatureFlags(): List<UserFeatureFlagResult> =
        userFeatureFlagCache.readAll().map(UserFeatureFlagResult::from)
}
