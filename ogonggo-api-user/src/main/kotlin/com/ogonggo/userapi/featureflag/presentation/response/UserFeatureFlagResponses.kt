package com.ogonggo.userapi.featureflag.presentation.response

import com.ogonggo.userapi.featureflag.business.UserFeatureFlagResult
import io.swagger.v3.oas.annotations.media.Schema

data class UserFeatureFlagListResponse(
    val items: List<UserFeatureFlagResponse>,
)

data class UserFeatureFlagResponse(
    @Schema(description = "플래그 key. 소문자 snake_case이며 코드가 이 문자열로 플래그를 찾습니다.", example = "ad_report_cost_efficiency")
    val key: String,
    @Schema(description = "켜져 있으면 true")
    val enabled: Boolean,
) {
    companion object {
        internal fun from(result: UserFeatureFlagResult): UserFeatureFlagResponse = UserFeatureFlagResponse(
            key = result.key,
            enabled = result.enabled,
        )
    }
}
