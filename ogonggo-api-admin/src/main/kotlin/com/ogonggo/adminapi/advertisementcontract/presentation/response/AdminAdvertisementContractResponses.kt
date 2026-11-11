package com.ogonggo.adminapi.advertisementcontract.presentation.response

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractDetailResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractProductResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractSummaryResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminBusinessAccountIssueResult
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAdvertisementContractSummaryResponse(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val targetId: Long?,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate?,
    val budgetAmount: Int?,
    val categories: List<AdvertisementProductCategory>,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementContractSummaryResult) = AdminAdvertisementContractSummaryResponse(
            id = result.id,
            source = result.source,
            status = result.status,
            promotionTarget = result.promotionTarget,
            targetId = result.targetId,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentDeadline = result.recruitmentDeadline,
            budgetAmount = result.budgetAmount,
            categories = result.categories,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class AdminAdvertisementContractDetailResponse(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val userId: Long?,
    val targetId: Long?,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String?,
    val recruitmentDeadline: LocalDate?,
    val purpose: AdvertisementContractPurpose?,
    val budgetAmount: Int?,
    val categories: List<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String?,
    val email: String?,
    val phoneNumber: String?,
    @field:Schema(description = "신청 폼에서 받은 사업자 번호 종류. 보내지 않은 신청과 비즈니스 공고 계약은 null입니다.", nullable = true)
    val businessNumberType: CompanyBusinessNumberType?,
    @field:Schema(description = "하이픈 없는 사업자 번호. 해외 기업이거나 보내지 않은 신청, 비즈니스 공고 계약은 null입니다.", nullable = true)
    val businessNumber: String?,
    val invoiceMethod: AdvertisementContractInvoiceMethod?,
    val creativeMethod: AdvertisementContractCreativeMethod?,
    @field:ArraySchema(
        arraySchema = Schema(
            description = "신청 폼에서 칸이 따로 없는 문항의 답. 받은 순서대로입니다. 답 없이 낸 신청, 이 값이 생기기 전 신청, 비즈니스 공고 계약은 null입니다.",
            nullable = true,
        ),
    )
    val formAnswers: List<AdminAdvertisementContractFormAnswerResponse>?,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val products: List<AdminAdvertisementContractProductResponse>,
) {
    companion object {
        fun from(result: AdminAdvertisementContractDetailResult) = AdminAdvertisementContractDetailResponse(
            id = result.id,
            source = result.source,
            status = result.status,
            userId = result.userId,
            targetId = result.targetId,
            promotionTarget = result.promotionTarget,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentUrl = result.recruitmentUrl,
            recruitmentDeadline = result.recruitmentDeadline,
            purpose = result.purpose,
            budgetAmount = result.budgetAmount,
            categories = result.categories,
            scheduleRequest = result.scheduleRequest,
            advertisementRequest = result.advertisementRequest,
            managerName = result.managerName,
            email = result.email,
            phoneNumber = result.phoneNumber,
            businessNumberType = result.businessNumberType,
            businessNumber = result.businessNumber,
            invoiceMethod = result.invoiceMethod,
            creativeMethod = result.creativeMethod,
            formAnswers = result.formAnswers?.map(AdminAdvertisementContractFormAnswerResponse::from),
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
            products = result.products.map(AdminAdvertisementContractProductResponse::from),
        )
    }
}

data class AdminAdvertisementContractFormAnswerResponse(
    @field:Schema(description = "문항 키")
    val key: String,
    @field:Schema(description = "신청 당시 화면에 보인 문항 이름")
    val label: String,
    val value: String,
) {
    companion object {
        fun from(answer: AdvertisementContractFormAnswer) = AdminAdvertisementContractFormAnswerResponse(
            key = answer.key,
            label = answer.label,
            value = answer.value,
        )
    }
}

data class AdminAdvertisementContractProductResponse(
    val contractProductId: Long,
    val productId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val productDeleted: Boolean,
    val addedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementContractProductResult) = AdminAdvertisementContractProductResponse(
            contractProductId = result.contractProductId,
            productId = result.productId,
            productName = result.productName,
            category = result.category,
            productDeleted = result.productDeleted,
            addedAt = result.addedAt,
        )
    }
}

data class CreateAdminAdvertisementContractProductResponse(
    val id: Long,
)

data class AdminBusinessAccountIssueResponse(
    @field:Schema(description = "신청을 연결한 비즈니스 회원 식별자")
    val userId: Long,
    @field:Schema(description = "이번에 새로 만들었으면 true. 이때만 임시 비밀번호 메일을 보낸다")
    val created: Boolean,
    @field:Schema(description = "이번에 회원에 연결한 비회원 신청 수. 이 계약을 포함한다")
    val linkedContractCount: Int,
) {
    companion object {
        fun from(result: AdminBusinessAccountIssueResult) = AdminBusinessAccountIssueResponse(
            userId = result.userId,
            created = result.created,
            linkedContractCount = result.linkedContractCount,
        )
    }
}
