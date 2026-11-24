package com.ogonggo.adminapi.featureflag.presentation.response

import com.ogonggo.adminapi.featureflag.business.AdminFeatureFlagResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminFeatureFlagListResponse(
    val items: List<AdminFeatureFlagResponse>,
)

data class AdminFeatureFlagResponse(
    @Schema(description = "플래그 key. 소문자 snake_case이며 코드가 이 문자열로 플래그를 찾습니다.", example = "ad_report_cost_efficiency")
    val key: String,
    @Schema(description = "켜져 있으면 true")
    val enabled: Boolean,
    @Schema(description = "이 플래그가 무엇을 켜고 끄는지")
    val description: String,
    @Schema(description = "마지막으로 바꾼 일시. 바꾼 적이 없으면 플래그를 만든 일시입니다.")
    val updatedAt: LocalDateTime,
    @Schema(description = "켜짐 여부를 마지막으로 바꾼 관리자의 회원 식별자. 바꾼 적이 없으면 null입니다.", nullable = true)
    val updatedBy: Long?,
) {
    companion object {
        internal fun from(result: AdminFeatureFlagResult): AdminFeatureFlagResponse = AdminFeatureFlagResponse(
            key = result.key,
            enabled = result.enabled,
            description = result.description,
            updatedAt = result.updatedAt,
            updatedBy = result.updatedBy,
        )
    }
}
