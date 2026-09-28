package com.ogonggo.adminapi.bootcamp.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "크롤러 부트캠프 등록 결과")
data class CrawlerBootcampRegistrationResponse(
    @field:Schema(description = "등록된 부트캠프 식별자")
    val bootcampId: Long,
)

@Schema(description = "원문 URL로 찾은 크롤러 부트캠프")
data class CrawlerBootcampLookupResponse(
    @field:Schema(description = "부트캠프 식별자")
    val bootcampId: Long,
)
