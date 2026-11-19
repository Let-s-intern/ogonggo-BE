package com.ogonggo.userapi.advertisementcontract.presentation.response

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractReportUnavailableReason
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractDetailResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractProductResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractSummaryResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class MyAdvertisementContractSummaryResponse(
    val id: Long,
    @field:Schema(description = "계약이 생긴 경로. 광고 신청 폼(APPLICATION_FORM) 또는 비즈니스 공고 등록(COMPANY_POSTING)")
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    @field:Schema(description = "공고명. 비즈니스 공고 계약은 공고를 등록할 때의 제목")
    val recruitmentTitle: String,
    @field:Schema(description = "모집 마감일. 비즈니스 상시 공고는 null", nullable = true)
    val recruitmentDeadline: LocalDate?,
    @field:Schema(description = "신청 때 고른 광고 카테고리. 비즈니스 공고 계약은 빈 배열")
    val categories: List<AdvertisementProductCategory>,
    @field:Schema(description = "협의로 정한 결제 금액(원). 아직 정하지 않았으면 null", nullable = true)
    val paymentAmount: Int?,
    @field:Schema(description = "신청 일시. 비즈니스 공고 계약은 공고 등록 일시")
    val registeredAt: LocalDateTime,
) {
    companion object {
        fun from(result: MyAdvertisementContractSummaryResult) = MyAdvertisementContractSummaryResponse(
            id = result.id,
            source = result.source,
            status = result.status,
            promotionTarget = result.promotionTarget,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentDeadline = result.recruitmentDeadline,
            categories = result.categories,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
        )
    }
}

data class MyAdvertisementContractDetailResponse(
    val id: Long,
    @field:Schema(description = "계약이 생긴 경로. 광고 신청 폼(APPLICATION_FORM) 또는 비즈니스 공고 등록(COMPANY_POSTING)")
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    @field:Schema(description = "공고명. 비즈니스 공고 계약은 공고를 등록할 때의 제목")
    val recruitmentTitle: String,
    @field:Schema(description = "공고·지원 링크. 비즈니스 공고 계약은 null", nullable = true)
    val recruitmentUrl: String?,
    @field:Schema(description = "모집 마감일. 비즈니스 상시 공고는 null", nullable = true)
    val recruitmentDeadline: LocalDate?,
    @field:Schema(description = "광고 목적. 비즈니스 공고 계약은 null", nullable = true)
    val purpose: AdvertisementContractPurpose?,
    @field:Schema(description = "예정 예산(원). 적지 않았으면 null", nullable = true)
    val budgetAmount: Int?,
    @field:Schema(description = "신청 때 고른 광고 카테고리")
    val categories: List<AdvertisementProductCategory>,
    @field:Schema(description = "일정 요청사항. 비워 보냈으면 null", nullable = true)
    val scheduleRequest: String?,
    @field:Schema(description = "광고 요청사항. 비워 보냈으면 null", nullable = true)
    val advertisementRequest: String?,
    @field:Schema(description = "담당자 이름. 비즈니스 공고 계약은 null(회원의 기업 정보를 본다)", nullable = true)
    val managerName: String?,
    @field:Schema(description = "담당자 이메일. 비즈니스 공고 계약은 null", nullable = true)
    val email: String?,
    @field:Schema(description = "담당자 연락처. 비즈니스 공고 계약은 null", nullable = true)
    val phoneNumber: String?,
    @field:Schema(description = "사업자 번호 종류. 보내지 않았으면 null", nullable = true)
    val businessNumberType: CompanyBusinessNumberType?,
    @field:Schema(description = "사업자 번호(숫자만). 보내지 않았거나 해외 기업이면 null", nullable = true)
    val businessNumber: String?,
    @field:Schema(description = "계산서 발행 방식. 비즈니스 공고 계약은 null", nullable = true)
    val invoiceMethod: AdvertisementContractInvoiceMethod?,
    @field:Schema(description = "소재 준비 방식. 비즈니스 공고 계약은 null", nullable = true)
    val creativeMethod: AdvertisementContractCreativeMethod?,
    @field:ArraySchema(
        arraySchema = Schema(description = "칸이 따로 없는 폼 문항의 답. 보낸 순서대로. 답 없이 낸 신청과 이 값이 생기기 전 신청은 null", nullable = true),
    )
    val formAnswers: List<MyAdvertisementContractFormAnswerResponse>?,
    @field:Schema(description = "협의로 정한 결제 금액(원). 아직 정하지 않았으면 null", nullable = true)
    val paymentAmount: Int?,
    @field:Schema(description = "신청 일시")
    val registeredAt: LocalDateTime,
    @field:Schema(description = "운영자가 계약에 붙인 광고 상품. 붙인 순서이며 아직 없으면 빈 배열")
    val products: List<MyAdvertisementContractProductResponse>,
) {
    companion object {
        fun from(result: MyAdvertisementContractDetailResult) = MyAdvertisementContractDetailResponse(
            id = result.id,
            source = result.source,
            status = result.status,
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
            formAnswers = result.formAnswers?.map(MyAdvertisementContractFormAnswerResponse::from),
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            products = result.products.map(MyAdvertisementContractProductResponse::from),
        )
    }
}

data class MyAdvertisementContractFormAnswerResponse(
    @field:Schema(description = "문항 키")
    val key: String,
    @field:Schema(description = "신청 당시 화면에 보인 문항 이름")
    val label: String,
    val value: String,
) {
    companion object {
        fun from(answer: AdvertisementContractFormAnswer) = MyAdvertisementContractFormAnswerResponse(
            key = answer.key,
            label = answer.label,
            value = answer.value,
        )
    }
}

data class MyAdvertisementContractProductResponse(
    @field:Schema(description = "계약에 붙인 상품 연결 식별자. 같은 상품을 여러 번 붙이면 연결마다 다르다")
    val contractProductId: Long,
    @field:Schema(description = "조회 시점의 상품 이름")
    val productName: String,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "노출 자리. 외부 채널이거나 자리가 없는 상품은 null", nullable = true)
    val position: AdvertisementProductPosition?,
    @field:Schema(description = "운영자가 상품을 지웠는지. 지운 상품도 계약에 붙어 있으면 이름과 함께 준다")
    val productDeleted: Boolean,
    @field:Schema(description = "게시 시작일. 게시 기간은 데이터 집계 기간이며 리포트는 이 기간의 데이터만 보여 줍니다. 기간을 넣기 전에 붙인 상품은 null", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "게시 종료일(이 날까지 게시). 게시 기간은 데이터 집계 기간이며 리포트는 이 기간의 데이터만 보여 줍니다. 기간을 넣기 전에 붙인 상품은 null", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "성과 리포트를 열 수 있는 상품인지")
    val reportAvailable: Boolean,
    @field:Schema(description = "리포트를 열 수 없는 이유. 기간 없음(NO_PERIOD), 채널 상품(NOT_EXPOSABLE). 열 수 있으면 null", nullable = true)
    val reportUnavailableReason: AdvertisementContractReportUnavailableReason?,
) {
    companion object {
        fun from(result: MyAdvertisementContractProductResult) = MyAdvertisementContractProductResponse(
            contractProductId = result.contractProductId,
            productName = result.productName,
            category = result.category,
            position = result.position,
            productDeleted = result.productDeleted,
            startDate = result.startDate,
            endDate = result.endDate,
            reportAvailable = result.reportAvailable,
            reportUnavailableReason = result.reportUnavailableReason,
        )
    }
}
