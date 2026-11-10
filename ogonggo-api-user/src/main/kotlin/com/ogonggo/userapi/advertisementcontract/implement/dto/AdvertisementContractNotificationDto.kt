package com.ogonggo.userapi.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand

/** 계약 접수 메일·Slack 알림을 만드는 Implement 전달 타입이다. 자유 입력 요청사항은 외부 채널에 포함하지 않는다. */
data class AdvertisementContractNotificationDto(
    val contractId: Long,
    val promotionTarget: String,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: String,
    val purpose: String,
    /** 예정 예산을 적은 신청만 있다. 천 단위 쉼표를 넣은 원 금액(1,500,000원)이다. */
    val budgetAmount: String?,
    val categories: List<String>,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    /** 신청자가 보낸 경우만 있다. 번호 종류와 번호를 함께 쓰고(사업자등록번호 1234567890), 해외 기업이면 종류만 쓴다. */
    val businessNumber: String? = null,
) {
    fun templateParameters(): Map<String, String> = buildMap {
        put("contractId", contractId.toString())
        put("promotionTarget", promotionTarget)
        put("companyName", companyName)
        put("recruitmentTitle", recruitmentTitle)
        put("recruitmentDeadline", recruitmentDeadline)
        put("purpose", purpose)
        budgetAmount?.let { put("budgetAmount", it) }
        put("categories", categories.joinToString(" · "))
        put("managerName", managerName)
        put("email", email)
        put("phoneNumber", phoneNumber)
        businessNumber?.let { put("businessNumber", it) }
    }

    companion object {
        fun from(contractId: Long, command: CreateAdvertisementContractCommand) = AdvertisementContractNotificationDto(
            contractId = contractId,
            promotionTarget = command.promotionTarget.desc,
            companyName = command.companyName,
            recruitmentTitle = command.recruitmentTitle,
            recruitmentDeadline = command.recruitmentDeadline.toString(),
            purpose = command.purpose.desc,
            budgetAmount = command.budgetAmount?.let { "%,d원".format(it) },
            categories = command.categories.sortedBy { it.code }.map { it.desc },
            managerName = command.managerName,
            email = command.email,
            phoneNumber = command.phoneNumber,
            businessNumber = command.businessNumberType?.let { type ->
                listOfNotNull(type.desc, command.businessNumber).joinToString(" ")
            },
        )
    }
}
