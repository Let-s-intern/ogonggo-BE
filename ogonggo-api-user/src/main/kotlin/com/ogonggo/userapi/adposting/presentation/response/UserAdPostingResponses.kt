package com.ogonggo.userapi.adposting.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.userapi.adposting.business.UserAdPostingResult
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import io.swagger.v3.oas.annotations.media.Schema

data class UserAdPostingResponse(
    @Schema(description = "광고 공고 id. 공고·부트캠프 id가 아닙니다.")
    val id: Long,
    val placement: AdPostingPlacement,
    @Schema(description = "자리가 정합니다. BOOTCAMP_LIST만 BOOTCAMP입니다.")
    val targetType: AdPostingTargetType,
    @Schema(description = "targetType이 JOB이면 있습니다. GET /api/v1/jobs 목록 항목과 같고 bookmarked는 늘 false입니다.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val job: UserJobSummaryResponse?,
    @Schema(description = "targetType이 BOOTCAMP면 있습니다. GET /api/v1/bootcamps 목록 항목과 같고 bookmarked는 늘 false입니다.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val bootcamp: UserBootcampSummaryResponse?,
) {
    companion object {
        internal fun from(result: UserAdPostingResult): UserAdPostingResponse = UserAdPostingResponse(
            id = result.id,
            placement = result.placement,
            targetType = result.targetType,
            job = result.job?.let(UserJobSummaryResponse::from),
            bootcamp = result.bootcamp?.let(UserBootcampSummaryResponse::from),
        )
    }
}
