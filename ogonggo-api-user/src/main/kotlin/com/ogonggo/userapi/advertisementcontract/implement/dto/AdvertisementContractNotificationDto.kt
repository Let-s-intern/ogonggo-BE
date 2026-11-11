package com.ogonggo.userapi.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand

/**
 * 계약 접수 메일·Slack 알림을 만드는 Implement 전달 타입이다. 자유 입력 요청사항(일정·광고 요청사항)은 외부 채널에 포함하지 않는다.
 * 폼 답변은 영업이 신청 내용을 바로 볼 수 있도록 Slack에 싣는다(LC-3607). 메일 템플릿은 이 값을 쓰지 않는다.
 */
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
    /**
     * 폼 답변을 받은 순서대로 한 줄에 하나씩 `문항 이름: 답`으로 쓴 값이다. 답이 없는 신청은 없다.
     * payload는 문자열 값만 담는 지도이고 `notifications.payload_json`(TEXT)에 저장되므로 [MAX_FORM_ANSWERS_LENGTH]자까지만 싣는다.
     */
    val formAnswers: String? = null,
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
        formAnswers?.let { put("formAnswers", it) }
    }

    companion object {
        /** Slack section 본문 한도(3000자)보다 작게 잡는다. 넘는 부분은 어드민 상세에서 본다. */
        const val MAX_FORM_ANSWERS_LENGTH = 2500
        private const val OMITTED = "\n…(이하 생략, 어드민에서 확인)"

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
            formAnswers = command.formAnswers?.takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { "${it.label}: ${it.value}" }
                ?.let { if (it.length <= MAX_FORM_ANSWERS_LENGTH) it else it.take(MAX_FORM_ANSWERS_LENGTH) + OMITTED },
        )
    }
}
