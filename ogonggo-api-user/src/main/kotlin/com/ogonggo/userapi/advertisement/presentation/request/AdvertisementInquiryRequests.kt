package com.ogonggo.userapi.advertisement.presentation.request

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryInflowCommand
import com.ogonggo.userapi.advertisement.business.CreateAdvertisementInquiryCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 비로그인으로 열려 있는 폼이라 길이 상한이 방어선 역할도 한다.
 * 상한이 없으면 거대한 본문이 그대로 슬랙 메시지가 되어 채널을 덮는다.
 *
 * 직함·채용 직무명·공고 링크·마케팅 동의·유입 원본은 선택이다. 이 필드가 생기기 전의 폼(`apps/launch-notice`)이
 * 이전될 때까지 같은 API를 부르므로 필수로 두면 그 폼의 접수가 끊긴다. 새 폼은 화면에서 필수로 받는다.
 * 회원 구분과 회원 식별자는 받지 않는다. 서버가 토큰으로 정하며 보내도 무시한다.
 */
data class CreateAdvertisementInquiryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val companyName: String,
    @field:NotBlank
    @field:Size(max = 50)
    val managerName: String,
    @Schema(description = "담당자 직함")
    @field:Size(max = 50)
    val managerTitle: String? = null,
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    /** 대표번호와 내선 표기가 다양해 형식은 검증하지 않고 길이만 제한한다. */
    @field:NotBlank
    @field:Size(max = 20)
    val phoneNumber: String,
    @field:NotNull
    val inquiryType: AdvertisementInquiryType,
    /** 폼에서 한 곳만 고르므로 목록이 아니라 단일 값으로 받는다. */
    @field:NotNull
    val promotionChannel: AdvertisementInquiryPromotionChannel,
    @Schema(description = "채용 직무명")
    @field:Size(max = 100)
    val hiringJobName: String? = null,
    @Schema(description = "채용공고 링크")
    @field:Size(max = 500)
    val jobPostingUrl: String? = null,
    /** 슬랙 section 블록의 3000자 제한 안에 들어가도록 상한을 둔다. */
    @Schema(description = "현재 사용 중인 채용 홍보 채널과 어려운 점")
    @field:NotBlank
    @field:Size(max = 2000)
    val promotionAnswer: String,
    @Schema(description = "마케팅 정보 수신 동의. 보내지 않으면 동의하지 않은 것으로 저장합니다.")
    val marketingConsent: Boolean? = null,
    @Schema(description = "진입 URL의 utm_source. 유입 경로 판정에 씁니다.")
    @field:Size(max = 100)
    val utmSource: String? = null,
    @Schema(description = "진입 URL의 utm_medium")
    @field:Size(max = 100)
    val utmMedium: String? = null,
    @Schema(description = "진입 URL의 utm_campaign")
    @field:Size(max = 100)
    val utmCampaign: String? = null,
    @Schema(description = "진입 URL의 utm_content")
    @field:Size(max = 100)
    val utmContent: String? = null,
    @Schema(description = "페이지에 처음 들어왔을 때의 document.referrer 원문. 유입 경로 판정에 씁니다.")
    @field:Size(max = 2000)
    val referrer: String? = null,
    @Schema(description = "처음 들어온 호스트. biz.ogonggo.co.kr(리다이렉트 전)인지 www.ogonggo.co.kr인지", example = "biz.ogonggo.co.kr")
    @field:Size(max = 255)
    val entryHost: String? = null,
    @Schema(description = "진입 URL의 경로와 쿼리 전체", example = "/biz?utm_source=coldmail&entry=biz")
    @field:Size(max = 2000)
    val landingPath: String? = null,
) {
    fun toCommand(): CreateAdvertisementInquiryCommand = CreateAdvertisementInquiryCommand(
        companyName = companyName,
        managerName = managerName,
        managerTitle = managerTitle,
        email = email,
        phoneNumber = phoneNumber,
        inquiryType = inquiryType,
        promotionChannel = promotionChannel,
        hiringJobName = hiringJobName,
        jobPostingUrl = jobPostingUrl,
        promotionAnswer = promotionAnswer,
        marketingConsent = marketingConsent ?: false,
        inflow = AdvertisementInquiryInflowCommand(
            utmSource = utmSource,
            utmMedium = utmMedium,
            utmCampaign = utmCampaign,
            utmContent = utmContent,
            referrer = referrer,
            entryHost = entryHost,
            landingPath = landingPath,
        ),
    )
}
