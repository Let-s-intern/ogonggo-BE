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
    val categories: List<String>,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
) {
    fun templateParameters(): Map<String, String> = mapOf(
        "contractId" to contractId.toString(),
        "promotionTarget" to promotionTarget,
        "companyName" to companyName,
        "recruitmentTitle" to recruitmentTitle,
        "recruitmentDeadline" to recruitmentDeadline,
        "purpose" to purpose,
        "categories" to categories.joinToString(" · "),
        "managerName" to managerName,
        "email" to email,
        "phoneNumber" to phoneNumber,
    )

    companion object {
        fun from(contractId: Long, command: CreateAdvertisementContractCommand) = AdvertisementContractNotificationDto(
            contractId = contractId,
            promotionTarget = command.promotionTarget.desc,
            companyName = command.companyName,
            recruitmentTitle = command.recruitmentTitle,
            recruitmentDeadline = command.recruitmentDeadline.toString(),
            purpose = command.purpose.desc,
            categories = command.categories.sortedBy { it.code }.map { it.desc },
            managerName = command.managerName,
            email = command.email,
            phoneNumber = command.phoneNumber,
        )
    }
}
