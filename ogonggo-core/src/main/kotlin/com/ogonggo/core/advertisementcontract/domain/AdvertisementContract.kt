package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.jpa.BaseTimeEntity
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 광고 계약이다. 신청부터 협의, 승인·반려까지 한 행이 상태([status])로 거친다. 신청을 따로 두지 않는다.
 *
 * 두 경로로 생긴다([source]).
 * - 광고 신청 폼: 로그인 없이도 신청할 수 있어 회원 식별자가 없을 수 있다. 폼이 받은 값을 모두 가진다.
 *   사업자 번호는 선택이다. 이 값을 보내기 전에 배포된 신청 화면이 있기 때문이다.
 * - 비즈니스 공고 등록: 기업회원이 공고를 등록하면 그 공고 하나에 계약 하나가 생기고, 공고 검수 상태를 따른다([followReview]).
 *
 * 신청자가 쓴 값은 남긴 그대로 두고, 운영자는 상태와 결제 금액만 바꾼다. 지우는 기능이 없어 소프트 삭제 칼럼을 두지 않는다.
 * 실제로 집행하는 상품은 운영자가 계약 상세에서 붙인다([AdvertisementContractProduct]).
 */
@Entity
@Table(
    name = "advertisement_contracts",
    indexes = [
        Index(name = "idx_advertisement_contracts_status_created_at", columnList = "status, created_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_advertisement_contracts_source_target",
            columnNames = ["source", "promotion_target", "target_id"],
        ),
    ],
)
class AdvertisementContract internal constructor(
    source: AdvertisementContractSource,
    status: AdvertisementContractStatus,
    userId: Long?,
    promotionTarget: AdvertisementContractPromotionTarget,
    targetId: Long?,
    companyName: String,
    recruitmentTitle: String,
    recruitmentUrl: String?,
    recruitmentDeadline: LocalDate?,
    purpose: AdvertisementContractPurpose?,
    budgetAmount: Int?,
    categories: Set<AdvertisementProductCategory>,
    scheduleRequest: String?,
    advertisementRequest: String?,
    managerName: String?,
    email: String?,
    phoneNumber: String?,
    businessNumberType: CompanyBusinessNumberType?,
    businessNumber: String?,
    invoiceMethod: AdvertisementContractInvoiceMethod?,
    creativeMethod: AdvertisementContractCreativeMethod?,
) : BaseTimeEntity() {

    init {
        require(userId == null || userId > 0) { "회원 식별자는 양수여야 합니다." }
        require(companyName.isNotBlank() && recruitmentTitle.isNotBlank()) { "기업명과 공고명은 비울 수 없습니다." }
        require(budgetAmount == null || budgetAmount > 0) { "예정 예산은 1원 이상이어야 합니다." }
        require((scheduleRequest?.length ?: 0) <= MAX_REQUEST_LENGTH && (advertisementRequest?.length ?: 0) <= MAX_REQUEST_LENGTH) {
            "요청사항은 ${MAX_REQUEST_LENGTH}자 이하여야 합니다."
        }
        when (source) {
            AdvertisementContractSource.APPLICATION_FORM -> {
                require(targetId == null) { "신청 폼 계약은 공고를 가리키지 않습니다." }
                require(!recruitmentUrl.isNullOrBlank() && recruitmentDeadline != null && purpose != null) {
                    "신청 폼 계약은 공고 링크, 모집 마감일, 광고 목적이 있어야 합니다."
                }
                require(!managerName.isNullOrBlank() && !email.isNullOrBlank() && !phoneNumber.isNullOrBlank()) {
                    "신청 폼 계약은 담당자 이름, 이메일, 연락처가 있어야 합니다."
                }
                require(invoiceMethod != null && creativeMethod != null) { "신청 폼 계약은 계산서 발행 방식과 소재 준비 방식이 있어야 합니다." }
                require(categories.isNotEmpty()) { "신청 폼 계약은 광고 카테고리를 하나 이상 골라야 합니다." }
                validateBusinessNumber(businessNumberType, businessNumber)
            }

            AdvertisementContractSource.COMPANY_POSTING -> {
                require(userId != null && targetId != null && targetId > 0) { "비즈니스 공고 계약은 등록한 회원과 공고를 가리켜야 합니다." }
                require(budgetAmount == null) { "비즈니스 공고 계약은 예정 예산을 갖지 않습니다." }
                require(businessNumberType == null && businessNumber == null) { "비즈니스 공고 계약은 사업자 번호를 갖지 않습니다." }
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 계약 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val source: AdvertisementContractSource = source /* 계약이 생긴 경로 */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AdvertisementContractStatus = status /* 계약 상태 */
        protected set

    @Column(name = "user_id")
    val userId: Long? = userId /* 신청한 회원 식별자. 비회원이면 없다 */

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_target", nullable = false, length = 20)
    val promotionTarget: AdvertisementContractPromotionTarget = promotionTarget /* 홍보 유형 */

    @Column(name = "target_id")
    val targetId: Long? = targetId /* 비즈니스 공고 계약이 가리키는 채용공고·부트캠프 식별자 */

    @Column(name = "company_name", nullable = false, length = 150)
    val companyName: String = companyName /* 기업·기관명 */

    @Column(name = "recruitment_title", nullable = false, length = 255)
    val recruitmentTitle: String = recruitmentTitle /* 공고명. 비즈니스 공고 계약은 등록 시점의 제목 */

    @Column(name = "recruitment_url", length = 2000)
    val recruitmentUrl: String? = recruitmentUrl /* 공고·지원 링크 */

    @Column(name = "recruitment_deadline")
    val recruitmentDeadline: LocalDate? = recruitmentDeadline /* 모집 마감일. 비즈니스 상시 공고는 없다 */

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    val purpose: AdvertisementContractPurpose? = purpose /* 광고 목적 */

    @Column(name = "budget_amount")
    val budgetAmount: Int? = budgetAmount /* 예정 예산(원). 신청 폼에서 적지 않을 수 있다. 실제 결제 금액(paymentAmount)과 다르다 */

    @ElementCollection
    @CollectionTable(name = "advertisement_contract_categories", joinColumns = [JoinColumn(name = "contract_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    var categories: MutableSet<AdvertisementProductCategory> = categories.toMutableSet() /* 신청 때 고른 광고 카테고리 */
        get() = field.toMutableSet()
        protected set

    @Column(name = "schedule_request", length = MAX_REQUEST_LENGTH)
    val scheduleRequest: String? = scheduleRequest /* 일정 요청사항 */

    @Column(name = "advertisement_request", length = MAX_REQUEST_LENGTH)
    val advertisementRequest: String? = advertisementRequest /* 광고 요청사항 */

    @Column(name = "manager_name", length = 50)
    val managerName: String? = managerName /* 담당자 이름 */

    @Column(length = 255)
    val email: String? = email /* 담당자 업무용 이메일 */

    @Column(name = "phone_number", length = 20)
    val phoneNumber: String? = phoneNumber /* 담당자 연락처 */

    @Enumerated(EnumType.STRING)
    @Column(name = "business_number_type", length = 30)
    val businessNumberType: CompanyBusinessNumberType? = businessNumberType /* 사업자 번호 종류. 해외 기업이면 OVERSEAS */

    @Column(name = "business_number", length = 13)
    val businessNumber: String? = businessNumber /* 사업자 번호. 숫자만 */

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_method", length = 20)
    val invoiceMethod: AdvertisementContractInvoiceMethod? = invoiceMethod /* 계약 및 계산서 발행 방식 */

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_method", length = 30)
    val creativeMethod: AdvertisementContractCreativeMethod? = creativeMethod /* 소재 준비 방식 */

    @Column(name = "payment_amount")
    var paymentAmount: Int? = null /* 협의로 정한 실제 결제 금액 */
        protected set

    /**
     * 신청 폼 계약의 상태를 운영자가 바꾼다. 영업이 잘못 바꾼 상태를 되돌릴 수 있도록 어느 상태로든 바꾼다.
     * 비즈니스 공고 계약은 공고 검수와 어긋나지 않도록 막는다. 검수 화면에서 승인·반려한다.
     */
    fun changeStatus(status: AdvertisementContractStatus) {
        if (source != AdvertisementContractSource.APPLICATION_FORM) {
            throw ConflictException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW)
        }
        this.status = status
    }

    /**
     * 비즈니스 공고 계약의 상태를 공고 검수 상태에 맞춘다.
     * 승인된 공고를 고쳐 다시 검수를 받는 동안에도 신청으로 돌아간다. 그동안 공고가 노출되지 않기 때문이다.
     */
    fun followReview(reviewStatus: ContentReviewStatus) {
        check(source == AdvertisementContractSource.COMPANY_POSTING) { "비즈니스 공고 계약만 검수 상태를 따릅니다." }
        status = AdvertisementContractStatus.of(reviewStatus)
    }

    fun changePaymentAmount(paymentAmount: Int?) {
        require(paymentAmount == null || paymentAmount >= 0) { "결제 금액은 0 이상이어야 합니다." }
        this.paymentAmount = paymentAmount
    }

    companion object {
        const val MAX_REQUEST_LENGTH = 2000

        /** 광고 신청 폼으로 들어온 계약이다. 신청으로 시작한다. 비회원이면 회원 식별자가 없다. */
        fun requestByForm(
            userId: Long?,
            promotionTarget: AdvertisementContractPromotionTarget,
            companyName: String,
            recruitmentTitle: String,
            recruitmentUrl: String,
            recruitmentDeadline: LocalDate,
            purpose: AdvertisementContractPurpose,
            budgetAmount: Int?,
            categories: Set<AdvertisementProductCategory>,
            scheduleRequest: String?,
            advertisementRequest: String?,
            managerName: String,
            email: String,
            phoneNumber: String,
            businessNumberType: CompanyBusinessNumberType? = null,
            businessNumber: String? = null,
            invoiceMethod: AdvertisementContractInvoiceMethod,
            creativeMethod: AdvertisementContractCreativeMethod,
        ): AdvertisementContract = AdvertisementContract(
            source = AdvertisementContractSource.APPLICATION_FORM,
            status = AdvertisementContractStatus.REQUESTED,
            userId = userId,
            promotionTarget = promotionTarget,
            targetId = null,
            companyName = companyName,
            recruitmentTitle = recruitmentTitle,
            recruitmentUrl = recruitmentUrl,
            recruitmentDeadline = recruitmentDeadline,
            purpose = purpose,
            budgetAmount = budgetAmount,
            categories = categories,
            scheduleRequest = scheduleRequest?.takeIf { it.isNotBlank() },
            advertisementRequest = advertisementRequest?.takeIf { it.isNotBlank() },
            managerName = managerName,
            email = email,
            phoneNumber = phoneNumber,
            businessNumberType = businessNumberType,
            businessNumber = businessNumber,
            invoiceMethod = invoiceMethod,
            creativeMethod = creativeMethod,
        )

        /** 비즈니스로 등록한 공고 하나에 대한 계약이다. 등록 직후 공고는 검수 대기라 신청이다. */
        fun ofCompanyPosting(
            userId: Long,
            promotionTarget: AdvertisementContractPromotionTarget,
            targetId: Long,
            companyName: String,
            recruitmentTitle: String,
            recruitmentDeadline: LocalDate?,
        ): AdvertisementContract = AdvertisementContract(
            source = AdvertisementContractSource.COMPANY_POSTING,
            status = AdvertisementContractStatus.REQUESTED,
            userId = userId,
            promotionTarget = promotionTarget,
            targetId = targetId,
            companyName = companyName,
            recruitmentTitle = recruitmentTitle,
            recruitmentUrl = null,
            recruitmentDeadline = recruitmentDeadline,
            purpose = null,
            budgetAmount = null,
            categories = emptySet(),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = null,
            email = null,
            phoneNumber = null,
            businessNumberType = null,
            businessNumber = null,
            invoiceMethod = null,
            creativeMethod = null,
        )
    }
}

/** 기업 회원가입과 같은 규칙이다. 번호는 종류가 있어야 뜻이 있고, 종류가 정해지면 그 종류의 형식을 따라야 한다. */
private fun validateBusinessNumber(type: CompanyBusinessNumberType?, number: String?) {
    if (type == null) {
        require(number == null) { "번호 종류 없이 사업자 번호를 둘 수 없습니다." }
        return
    }
    require(type.accepts(number)) { "${type.desc}의 형식이 아닙니다." }
}
