package com.ogonggo.userapi.advertisement.presentation.request

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.CreateAdvertisementInquiryCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 비로그인으로 열려 있는 폼이라 길이 상한이 방어선 역할도 한다.
 * 상한이 없으면 거대한 본문이 그대로 슬랙 메시지가 되어 채널을 덮는다.
 */
data class CreateAdvertisementInquiryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val companyName: String,
    @field:NotBlank
    @field:Size(max = 50)
    val managerName: String,
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
    /** 슬랙 section 블록의 3000자 제한 안에 들어가도록 상한을 둔다. */
    @field:NotBlank
    @field:Size(max = 2000)
    val promotionAnswer: String,
) {
    fun toCommand(): CreateAdvertisementInquiryCommand = CreateAdvertisementInquiryCommand(
        companyName = companyName,
        managerName = managerName,
        email = email,
        phoneNumber = phoneNumber,
        inquiryType = inquiryType,
        promotionAnswer = promotionAnswer,
    )
}
