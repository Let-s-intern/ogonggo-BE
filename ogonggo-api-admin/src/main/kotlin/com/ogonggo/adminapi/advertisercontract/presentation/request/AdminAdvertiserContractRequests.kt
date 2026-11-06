package com.ogonggo.adminapi.advertisercontract.presentation.request

import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractCreateCommand
import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractUpdateCommand
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminAdvertiserContractRequest(
    @Schema(description = "계약명. 50자 이하, 앞뒤 공백을 빼고 비면 null로 저장합니다.", nullable = true)
    val name: String? = null,
    @Schema(description = "광고 시작(KST, 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
    val startAt: LocalDateTime? = null,
    @Schema(description = "광고 끝(KST, 미포함). startAt보다 뒤여야 합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val endAt: LocalDateTime? = null,
    @Schema(description = "금액(원). 0 이상 9,999,999,999 이하의 정수", requiredMode = Schema.RequiredMode.REQUIRED)
    val amount: Long? = null,
    @Schema(description = "amount가 부가가치세 포함 금액인가", requiredMode = Schema.RequiredMode.REQUIRED)
    val vatIncluded: Boolean? = null,
    @Schema(description = "결제 완료 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    val paid: Boolean? = null,
    @Schema(description = "결제일(KST). paid가 false면 null로 저장합니다.", nullable = true)
    val paidDate: LocalDate? = null,
    @ArraySchema(
        arraySchema = Schema(description = "산 자리. 겹치지 않게 보내며 서버가 선언 순서로 정렬해 저장합니다. 보내지 않으면 빈 목록입니다."),
    )
    val products: List<AdContractProduct?>? = null,
    @Schema(description = "메모. 500자 이하, 앞뒤 공백을 빼고 비면 null로 저장합니다.", nullable = true)
    val memo: String? = null,
) {
    fun toCommand(): AdminAdvertiserContractCreateCommand = AdminAdvertiserContractCreateCommand(
        name = name,
        startAt = startAt,
        endAt = endAt,
        amount = amount,
        vatIncluded = vatIncluded,
        paid = paid,
        paidDate = paidDate,
        products = products,
        memo = memo,
    )
}

/**
 * 보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다. `name`, `paidDate`, `memo`는 null을 보내면 지웁니다.
 * 그 밖의 칸은 null이면 보내지 않은 것과 같습니다.
 */
data class UpdateAdminAdvertiserContractRequest(
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 계약명을 지웁니다.")
    val name: Optional<String>? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val amount: Long? = null,
    val vatIncluded: Boolean? = null,
    val paid: Boolean? = null,
    @Schema(implementation = LocalDate::class, nullable = true, description = "null을 보내면 결제일을 지웁니다.")
    val paidDate: Optional<LocalDate>? = null,
    val products: List<AdContractProduct?>? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 메모를 지웁니다.")
    val memo: Optional<String>? = null,
) {
    fun toCommand(): AdminAdvertiserContractUpdateCommand = AdminAdvertiserContractUpdateCommand(
        name = name,
        startAt = startAt,
        endAt = endAt,
        amount = amount,
        vatIncluded = vatIncluded,
        paid = paid,
        paidDate = paidDate,
        products = products,
        memo = memo,
    )
}
