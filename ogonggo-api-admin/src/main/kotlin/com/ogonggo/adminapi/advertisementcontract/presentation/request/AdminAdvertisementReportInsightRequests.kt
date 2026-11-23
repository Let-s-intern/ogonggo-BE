package com.ogonggo.adminapi.advertisementcontract.presentation.request

import com.ogonggo.core.advertisementcontract.domain.AdvertisementReportInsight
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDraftDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReplaceAdminAdvertisementReportInsightsRequest(
    @field:Schema(
        description = "인사이트 목록. 이 상품의 인사이트를 통째로 바꾸고 목록 순서가 보이는 순서입니다. 빈 목록이면 모두 걷어냅니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Valid
    @field:Size(max = AdvertisementReportInsight.MAX_COUNT)
    val insights: List<AdminAdvertisementReportInsightRequest>,
    @field:Schema(
        description = "true 이면 지금 기업 회원의 리포트에 보냅니다. false 이면 운영자만 보는 임시 저장이며, 이미 보낸 것도 숨겨집니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val published: Boolean,
) {
    fun toDrafts(): List<AdvertisementReportInsightDraftDto> = insights.map { AdvertisementReportInsightDraftDto(it.title, it.body) }
}

data class AdminAdvertisementReportInsightRequest(
    @field:Schema(description = "제목", example = "광고가 사실상 유일한 유입 경로였어요", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = AdvertisementReportInsight.MAX_TITLE_LENGTH)
    val title: String,
    @field:Schema(description = "본문", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = AdvertisementReportInsight.MAX_BODY_LENGTH)
    val body: String,
)
