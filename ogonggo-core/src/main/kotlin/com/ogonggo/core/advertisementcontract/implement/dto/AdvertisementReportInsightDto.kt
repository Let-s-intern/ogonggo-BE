package com.ogonggo.core.advertisementcontract.implement.dto

/** 리포트에 붙는 인사이트 하나다. [published]가 거짓이면 운영자만 보는 임시 저장이다. */
data class AdvertisementReportInsightDto(
    val title: String,
    val body: String,
    val published: Boolean,
)

/** 운영자가 쓴 인사이트 한 개다. 순서는 목록 순서다. */
data class AdvertisementReportInsightDraftDto(
    val title: String,
    val body: String,
)
