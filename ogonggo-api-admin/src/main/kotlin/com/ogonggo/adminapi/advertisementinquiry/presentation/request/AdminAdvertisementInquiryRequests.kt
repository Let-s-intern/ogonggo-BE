package com.ogonggo.adminapi.advertisementinquiry.presentation.request

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiry
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ChangeAdvertisementInquiryStatusRequest(
    @field:NotNull
    val status: AdvertisementInquiryStatus,
)

data class ChangeAdvertisementInquiryMemoRequest(
    @Schema(description = "영업 담당자 메모. 통째로 바꾸며 null이거나 공백만 있으면 메모를 지웁니다.")
    @field:Size(max = AdvertisementInquiry.MAX_MEMO_LENGTH)
    val memo: String?,
)
