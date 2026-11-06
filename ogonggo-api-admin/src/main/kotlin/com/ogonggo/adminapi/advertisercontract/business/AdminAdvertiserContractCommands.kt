package com.ogonggo.adminapi.advertisercontract.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 계약 등록 본문이다. 검증 전 값을 그대로 담는다. 산 자리를 보내지 않으면 빈 목록이다.
 * 요소가 null인 산 자리는 겹친 값과 같이 거절한다.
 */
data class AdminAdvertiserContractCreateCommand(
    val name: String?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val amount: Long?,
    val vatIncluded: Boolean?,
    val paid: Boolean?,
    val paidDate: LocalDate?,
    val products: List<AdContractProduct?>?,
    val memo: String?,
) {
    internal fun toDraft(): AdminAdvertiserContractDraft = AdminAdvertiserContractDraft(
        name = name,
        startAt = startAt,
        endAt = endAt,
        amount = amount,
        vatIncluded = vatIncluded,
        paid = paid,
        paidDate = paidDate,
        products = products.orEmpty(),
        memo = memo,
    )
}

/**
 * 계약 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`name`, `paidDate`, `memo`)은
 * 보내지 않으면 null, null을 보내면 빈 [Optional]이다.
 */
data class AdminAdvertiserContractUpdateCommand(
    val name: Optional<String>? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val amount: Long? = null,
    val vatIncluded: Boolean? = null,
    val paid: Boolean? = null,
    val paidDate: Optional<LocalDate>? = null,
    val products: List<AdContractProduct?>? = null,
    val memo: Optional<String>? = null,
) {
    /** 저장된 값 위에 보낸 칸을 얹는다. 검증은 합친 결과로 한다. */
    internal fun mergeInto(contract: AdvertiserContract): AdminAdvertiserContractDraft = AdminAdvertiserContractDraft(
        name = if (name == null) contract.name else name.orElse(null),
        startAt = startAt ?: contract.startAt,
        endAt = endAt ?: contract.endAt,
        amount = amount ?: contract.amount,
        vatIncluded = vatIncluded ?: contract.vatIncluded,
        paid = paid ?: contract.paid,
        paidDate = if (paidDate == null) contract.paidDate else paidDate.orElse(null),
        products = products ?: contract.products,
        memo = if (memo == null) contract.memo else memo.orElse(null),
    )
}

/** 등록 본문이나 저장된 값과 합친 수정 본문이다. 프런트 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다. */
internal data class AdminAdvertiserContractDraft(
    val name: String?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val amount: Long?,
    val vatIncluded: Boolean?,
    val paid: Boolean?,
    val paidDate: LocalDate?,
    val products: List<AdContractProduct?>,
    val memo: String?,
) {
    fun toTerms(): AdvertiserContractTerms {
        if ((name?.trim()?.length ?: 0) > AdvertiserContractTerms.MAX_NAME_LENGTH) {
            throw AdminConsoleBadRequestException.field("name", "${AdvertiserContractTerms.MAX_NAME_LENGTH}자까지 보낼 수 있습니다.")
        }
        val startAt = startAt ?: throw dateTimeError("startAt")
        val endAt = endAt ?: throw dateTimeError("endAt")
        if (!startAt.isBefore(endAt)) {
            throw AdminConsoleBadRequestException.field("endAt", "광고 끝은 시작보다 뒤여야 합니다.")
        }
        if (amount == null || amount !in 0..AdvertiserContractTerms.MAX_AMOUNT) {
            throw AdminConsoleBadRequestException.field("amount", "0 이상의 정수(원)로 보내 주세요.")
        }
        val vatIncluded = vatIncluded ?: throw booleanError("vatIncluded")
        val paid = paid ?: throw booleanError("paid")
        if (products.any { it == null } || products.distinct().size != products.size) {
            throw AdminConsoleBadRequestException.field(
                "products",
                "${AdContractProduct.entries.joinToString(", ") { it.name }} 가운데 겹치지 않게 보내 주세요.",
            )
        }
        if ((memo?.trim()?.length ?: 0) > AdvertiserContractTerms.MAX_MEMO_LENGTH) {
            throw AdminConsoleBadRequestException.field("memo", "${AdvertiserContractTerms.MAX_MEMO_LENGTH}자까지 보낼 수 있습니다.")
        }
        return AdvertiserContractTerms(
            name = name,
            startAt = startAt,
            endAt = endAt,
            amount = amount,
            vatIncluded = vatIncluded,
            paid = paid,
            paidDate = paidDate,
            products = products.filterNotNull(),
            memo = memo,
        )
    }

    private fun dateTimeError(fieldName: String) =
        AdminConsoleBadRequestException.field(fieldName, "YYYY-MM-DDTHH:mm:ss(KST) 로 보내 주세요.")

    private fun booleanError(fieldName: String) = AdminConsoleBadRequestException.field(fieldName, "참·거짓 값을 보내 주세요.")
}
