package com.ogonggo.userapi.advertisementexposure.presentation.response

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementPostingResult
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import io.swagger.v3.oas.annotations.media.Schema

data class AdvertisementPostingResponse(
    @field:Schema(description = "광고 노출 ID. 노출·클릭 이벤트의 유입 경로로 보냅니다.")
    val exposureId: Long,
    val position: AdvertisementProductPosition,
    @field:Schema(description = "채용공고 목록(JOB_LIST) 노출의 직군 탭. 다른 자리는 null입니다.", nullable = true)
    val jobField: JobField?,
    @field:Schema(
        description = "오공고 PICK(JOB_PICK) 광고 카드에 운영자 PICK과 같은 상자로 보이는 추천 문구 제목. JOB_PICK이면 늘 있고 다른 자리는 null입니다.",
        nullable = true,
    )
    val recommendationTitle: String?,
    @field:Schema(description = "추천 문구 제목 아래 설명. JOB_PICK이면 늘 있고 다른 자리는 null입니다.", nullable = true)
    val recommendationDescription: String?,
    @field:Schema(description = "같은 자리 안의 순서. 작을수록 먼저입니다.")
    val displayOrder: Int,
    @field:Schema(description = "카드에 \"광고\" 표시를 붙일지")
    val adLabel: Boolean,
    val targetType: AdvertisementExposureTargetType,
    @field:Schema(description = "targetType이 JOB이면 공개 채용공고 목록과 같은 요약, 아니면 null", nullable = true)
    val job: UserJobSummaryResponse?,
    @field:Schema(description = "targetType이 BOOTCAMP이면 공개 부트캠프 목록과 같은 요약, 아니면 null", nullable = true)
    val bootcamp: UserBootcampSummaryResponse?,
) {
    companion object {
        internal fun from(result: AdvertisementPostingResult) = AdvertisementPostingResponse(
            exposureId = result.exposureId,
            position = result.position,
            jobField = result.jobField,
            recommendationTitle = result.recommendationTitle,
            recommendationDescription = result.recommendationDescription,
            displayOrder = result.displayOrder,
            adLabel = result.adLabel,
            targetType = result.targetType,
            job = result.job?.let(UserJobSummaryResponse::from),
            bootcamp = result.bootcamp?.let(UserBootcampSummaryResponse::from),
        )
    }
}
