package com.ogonggo.adminapi.job.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "원문 URL로 찾은 크롤러 채용공고")
data class CrawlerJobLookupResponse(
    @field:Schema(description = "채용공고 식별자")
    val jobId: Long,
)
