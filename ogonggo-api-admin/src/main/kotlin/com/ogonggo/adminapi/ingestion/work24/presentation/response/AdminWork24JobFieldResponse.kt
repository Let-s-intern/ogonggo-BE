package com.ogonggo.adminapi.ingestion.work24.presentation.response

import com.ogonggo.core.job.domain.JobField
import io.swagger.v3.oas.annotations.media.Schema

data class AdminWork24JobFieldResponse(
    @field:Schema(description = "직군")
    val jobField: JobField,
    @field:Schema(description = "참이면 고용24 채용정보 수집에서 이 직군의 공고를 받는다")
    val enabled: Boolean,
) {
    companion object {
        fun listOf(settings: Map<JobField, Boolean>): List<AdminWork24JobFieldResponse> =
            settings.map { (jobField, enabled) -> AdminWork24JobFieldResponse(jobField, enabled) }
    }
}
