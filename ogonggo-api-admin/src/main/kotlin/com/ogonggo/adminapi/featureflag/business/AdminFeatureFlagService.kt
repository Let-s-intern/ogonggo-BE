package com.ogonggo.adminapi.featureflag.business

import com.ogonggo.core.featureflag.implement.FeatureFlagManager
import com.ogonggo.core.featureflag.implement.FeatureFlagReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminFeatureFlagService(
    private val featureFlagReader: FeatureFlagReader,
    private val featureFlagManager: FeatureFlagManager,
) {

    /** key 순으로 모든 플래그를 준다. */
    fun getFeatureFlags(): List<AdminFeatureFlagResult> =
        featureFlagReader.readAll().map(AdminFeatureFlagResult::from)

    /** 켜짐 여부를 바꾸고 요청한 관리자를 마지막으로 바꾼 사람으로 남긴다. 없는 key는 404다. */
    @Transactional
    fun changeFeatureFlagEnabled(adminUserId: Long, flagKey: String, enabled: Boolean) {
        val featureFlag = featureFlagReader.read(flagKey)
        featureFlagManager.changeEnabled(featureFlag, enabled, adminUserId)
    }
}
