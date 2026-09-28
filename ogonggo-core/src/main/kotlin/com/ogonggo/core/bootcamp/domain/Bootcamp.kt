package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.error.ReviewErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "bootcamps",
    indexes = [
        Index(name = "idx_bootcamps_review", columnList = "review_status, deleted_at"),
    ],
)
class Bootcamp internal constructor(
    ownerUserId: Long? = null,
    companyName: String,
    title: String,
    programType: String,
    operationType: OperationType,
    recruitmentType: BootcampRecruitmentType,
    recruitmentStartAt: LocalDateTime? = null,
    recruitmentEndAt: LocalDateTime? = null,
    programStartDate: LocalDate,
    programEndDate: LocalDate,
    capacity: Int? = null,
    tuitionType: TuitionType,
    tuitionAmount: Long? = null,
    representativeImageUrl: String,
    shortDescription: String,
    content: String,
    eligibilityAndSelectionProcess: String? = null,
    logoUrl: String? = null,
    instructorInfo: String? = null,
    programFeatures: String? = null,
    completionRequirements: String? = null,
    applicationMethod: ApplicationMethod,
    applicationUrl: String? = null,
    managerEmail: String? = null,
    inquiryUrl: String? = null,
    publicationStartAt: LocalDateTime? = null,
    publicationEndAt: LocalDateTime? = null,
    sourceUrl: String? = null,
    status: BootcampStatus = BootcampStatus.DRAFT,
    closedAt: LocalDateTime? = null,
    publicationStatus: BootcampPublicationStatus = BootcampPublicationStatus.DRAFT,
) : BaseTimeEntity() {

    init {
        require(ownerUserId == null || ownerUserId > 0) { "소유자 식별자는 양수여야 합니다." }
        require((status == BootcampStatus.CLOSED) == (closedAt != null)) {
            "모집 마감 상태와 마감 일시가 일치해야 합니다."
        }
        require(ownerUserId == null || publicationStatus != BootcampPublicationStatus.PUBLISHED) {
            "기업회원 부트캠프는 검수 승인 전에 게시할 수 없습니다."
        }
        validateBootcampValues(
            companyName = companyName,
            title = title,
            programType = programType,
            recruitmentType = recruitmentType,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = capacity,
            tuitionAmount = tuitionAmount,
            representativeImageUrl = representativeImageUrl,
            shortDescription = shortDescription,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            logoUrl = logoUrl,
            instructorInfo = instructorInfo,
            programFeatures = programFeatures,
            completionRequirements = completionRequirements,
            applicationMethod = applicationMethod,
            applicationUrl = applicationUrl,
            managerEmail = managerEmail,
            inquiryUrl = inquiryUrl,
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
        )
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 부트캠프 식별자 */
        protected set

    @Column(name = "owner_user_id")
    var ownerUserId: Long? = ownerUserId /* 부트캠프를 등록한 사용자 식별자 */
        protected set

    @Column(name = "company_name", nullable = false, length = 150)
    var companyName: String = companyName /* 운영 회사명 */
        protected set

    @Column(nullable = false, length = 255)
    var title: String = title /* 부트캠프 프로그램명 */
        protected set

    @Column(name = "program_type", nullable = false, length = 50)
    var programType: String = programType /* 프로그램 유형 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    var operationType: OperationType = operationType /* 진행 방식 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_type", nullable = false, length = 20)
    var recruitmentType: BootcampRecruitmentType = recruitmentType /* 부트캠프 모집 기간 유형 */
        protected set

    @Column(name = "recruitment_start_at")
    var recruitmentStartAt: LocalDateTime? = recruitmentStartAt /* 모집 시작 일시 */
        protected set

    @Column(name = "recruitment_end_at")
    var recruitmentEndAt: LocalDateTime? = recruitmentEndAt /* 모집 종료 일시 */
        protected set

    @Column(name = "program_start_date", nullable = false)
    var programStartDate: LocalDate = programStartDate /* 교육 시작일 */
        protected set

    @Column(name = "program_end_date", nullable = false)
    var programEndDate: LocalDate = programEndDate /* 교육 종료일 */
        protected set

    @Column
    var capacity: Int? = capacity /* 모집 정원 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "tuition_type", nullable = false, length = 30)
    var tuitionType: TuitionType = tuitionType /* 수강료 유형 */
        protected set

    @Column(name = "tuition_amount")
    var tuitionAmount: Long? = tuitionAmount /* 수강료 */
        protected set

    @Column(name = "representative_image_url", nullable = false, length = 2048)
    var representativeImageUrl: String = representativeImageUrl /* 공고 대표 이미지 URL */
        protected set

    @Column(name = "short_description", nullable = false, length = 500)
    var shortDescription: String = shortDescription /* 공고 한 줄 소개 */
        protected set

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    var content: String = content /* 부트캠프 상세 내용 */
        protected set

    @Column(name = "eligibility_and_selection_process", columnDefinition = "LONGTEXT")
    var eligibilityAndSelectionProcess: String? = eligibilityAndSelectionProcess /* 지원 자격 및 전형 안내 */
        protected set

    /** 기업회원이 대표 이미지와 따로 올리는 로고다. 수집한 부트캠프는 값이 없다. */
    @Column(name = "logo_url", length = 2048)
    var logoUrl: String? = logoUrl /* 운영 회사 로고 이미지 URL */
        protected set

    @Column(name = "instructor_info", columnDefinition = "LONGTEXT")
    var instructorInfo: String? = instructorInfo /* 강사 정보 */
        protected set

    @Column(name = "program_features", columnDefinition = "LONGTEXT")
    var programFeatures: String? = programFeatures /* 교육 특징 */
        protected set

    @Column(name = "completion_requirements", columnDefinition = "LONGTEXT")
    var completionRequirements: String? = completionRequirements /* 수료 조건 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "application_method", nullable = false, length = 20)
    var applicationMethod: ApplicationMethod = applicationMethod /* 지원 방법 */
        protected set

    @Column(name = "application_url", length = 2048)
    var applicationUrl: String? = applicationUrl /* 외부 지원 페이지 URL */
        protected set

    @Column(name = "manager_email", length = 320)
    var managerEmail: String? = managerEmail /* 담당자 이메일 */
        protected set

    @Column(name = "inquiry_url", length = 2048)
    var inquiryUrl: String? = inquiryUrl /* 문의 링크 */
        protected set

    @Column(name = "publication_start_at")
    var publicationStartAt: LocalDateTime? = publicationStartAt /* 공고 공개 시작 일시 */
        protected set

    @Column(name = "publication_end_at")
    var publicationEndAt: LocalDateTime? = publicationEndAt /* 공고 공개 종료 일시 */
        protected set

    @Column(name = "source_url", length = 2048)
    var sourceUrl: String? = sourceUrl /* 부트캠프 원문 URL */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BootcampStatus = status /* 부트캠프 모집 상태 */
        protected set

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = closedAt /* 모집 마감 처리 일시 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    var publicationStatus: BootcampPublicationStatus = publicationStatus /* 부트캠프 게시 상태 */
        protected set

    /** 기업회원이 올린 부트캠프만 검수하므로 등록할 때 검수 대기로 시작한다. 소유자가 없으면 값이 없다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    var reviewStatus: ReviewStatus? = if (ownerUserId == null) null else ReviewStatus.PENDING /* 검수 상태 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 부트캠프 삭제 일시 */
        protected set

    fun update(
        companyName: String,
        title: String,
        programType: String,
        operationType: OperationType,
        recruitmentType: BootcampRecruitmentType,
        recruitmentStartAt: LocalDateTime?,
        recruitmentEndAt: LocalDateTime?,
        programStartDate: LocalDate,
        programEndDate: LocalDate,
        capacity: Int?,
        tuitionType: TuitionType,
        tuitionAmount: Long?,
        representativeImageUrl: String,
        shortDescription: String,
        content: String,
        eligibilityAndSelectionProcess: String?,
        logoUrl: String?,
        instructorInfo: String?,
        programFeatures: String?,
        completionRequirements: String?,
        applicationMethod: ApplicationMethod,
        applicationUrl: String?,
        managerEmail: String?,
        inquiryUrl: String?,
        publicationStartAt: LocalDateTime?,
        publicationEndAt: LocalDateTime?,
        sourceUrl: String?,
    ) {
        checkModifiable()
        validateBootcampValues(
            companyName = companyName,
            title = title,
            programType = programType,
            recruitmentType = recruitmentType,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = capacity,
            tuitionAmount = tuitionAmount,
            representativeImageUrl = representativeImageUrl,
            shortDescription = shortDescription,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            logoUrl = logoUrl,
            instructorInfo = instructorInfo,
            programFeatures = programFeatures,
            completionRequirements = completionRequirements,
            applicationMethod = applicationMethod,
            applicationUrl = applicationUrl,
            managerEmail = managerEmail,
            inquiryUrl = inquiryUrl,
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
        )

        this.companyName = companyName
        this.title = title
        this.programType = programType
        this.operationType = operationType
        this.recruitmentType = recruitmentType
        this.recruitmentStartAt = recruitmentStartAt
        this.recruitmentEndAt = recruitmentEndAt
        this.programStartDate = programStartDate
        this.programEndDate = programEndDate
        this.capacity = capacity
        this.tuitionType = tuitionType
        this.tuitionAmount = tuitionAmount
        this.representativeImageUrl = representativeImageUrl
        this.shortDescription = shortDescription
        this.content = content
        this.eligibilityAndSelectionProcess = eligibilityAndSelectionProcess
        this.logoUrl = logoUrl
        this.instructorInfo = instructorInfo
        this.programFeatures = programFeatures
        this.completionRequirements = completionRequirements
        this.applicationMethod = applicationMethod
        this.applicationUrl = applicationUrl
        this.managerEmail = managerEmail
        this.inquiryUrl = inquiryUrl
        this.publicationStartAt = publicationStartAt
        this.publicationEndAt = publicationEndAt
        this.sourceUrl = sourceUrl
    }

    /** 제목과 본문 칸 중 넘어온 것만 바꾼다. 본문 값이 null이면 그 칸을 비우며, 상세 내용은 비울 수 없다. */
    fun editContent(title: String?, contents: Map<BootcampContentField, String?>) {
        checkModifiable()
        require(title == null || title.isNotBlank()) { "부트캠프 프로그램명은 비어 있을 수 없습니다." }
        require(contents.values.all { it == null || it.isNotBlank() }) { "본문 칸은 공백일 수 없습니다." }
        require(contents.none { (field, value) -> field.required && value == null }) { "비울 수 없는 칸입니다." }

        title?.let { this.title = it }
        contents.forEach { (field, value) ->
            when (field) {
                BootcampContentField.CONTENT -> content = checkNotNull(value)
                BootcampContentField.ELIGIBILITY_AND_SELECTION_PROCESS -> eligibilityAndSelectionProcess = value
            }
        }
    }

    fun contentOf(field: BootcampContentField): String? = when (field) {
        BootcampContentField.CONTENT -> content
        BootcampContentField.ELIGIBILITY_AND_SELECTION_PROCESS -> eligibilityAndSelectionProcess
    }

    fun startRecruitment() {
        checkNotDeleted()
        if (status == BootcampStatus.RECRUITING) {
            return
        }
        status = BootcampStatus.RECRUITING
        closedAt = null
    }

    fun close(now: LocalDateTime) {
        checkNotDeleted()
        when (status) {
            BootcampStatus.DRAFT -> throw ConflictException(BootcampErrorCode.INVALID_BOOTCAMP_STATUS_TRANSITION)
            BootcampStatus.RECRUITING -> {
                status = BootcampStatus.CLOSED
                closedAt = now
            }
            BootcampStatus.CLOSED -> Unit
        }
    }

    /** 기업회원 부트캠프는 검수 승인을 받아야만 노출한다. 게시하는 쪽이 누구든 같은 규칙을 따른다. */
    fun publish() {
        checkModifiable()
        if (reviewStatus != null && reviewStatus != ReviewStatus.APPROVED) {
            throw ConflictException(ReviewErrorCode.REVIEW_NOT_APPROVED)
        }
        publicationStatus = BootcampPublicationStatus.PUBLISHED
    }

    fun hide() {
        checkModifiable()
        publicationStatus = BootcampPublicationStatus.HIDDEN
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    /** 승인하면 곧바로 노출한다. 승인 결과를 알릴 경로가 없어 다시 게시하게 하면 부트캠프가 비노출로 남는다. */
    fun approveReview() {
        checkReviewable()
        reviewStatus = ReviewStatus.APPROVED
        publicationStatus = BootcampPublicationStatus.PUBLISHED
    }

    fun rejectReview() {
        checkReviewable()
        reviewStatus = ReviewStatus.REJECTED
        unpublish()
    }

    /** 기업회원이 내용을 고치거나 운영자가 판정을 되돌리면 다시 검수를 기다리며, 그동안 노출하지 않는다. */
    fun requestReview() {
        checkReviewable()
        reviewStatus = ReviewStatus.PENDING
        unpublish()
    }

    private fun unpublish() {
        if (publicationStatus == BootcampPublicationStatus.PUBLISHED) {
            publicationStatus = BootcampPublicationStatus.HIDDEN
        }
    }

    private fun checkReviewable() {
        checkModifiable()
        if (reviewStatus == null) {
            throw ConflictException(ReviewErrorCode.CONTENT_NOT_REVIEWABLE)
        }
    }

    private fun checkModifiable() {
        checkNotDeleted()
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 부트캠프는 변경할 수 없습니다." }
    }
}

private fun validateBootcampValues(
    companyName: String,
    title: String,
    programType: String,
    recruitmentType: BootcampRecruitmentType,
    recruitmentStartAt: LocalDateTime?,
    recruitmentEndAt: LocalDateTime?,
    programStartDate: LocalDate,
    programEndDate: LocalDate,
    capacity: Int?,
    tuitionAmount: Long?,
    representativeImageUrl: String,
    shortDescription: String,
    content: String,
    eligibilityAndSelectionProcess: String?,
    logoUrl: String?,
    instructorInfo: String?,
    programFeatures: String?,
    completionRequirements: String?,
    applicationMethod: ApplicationMethod,
    applicationUrl: String?,
    managerEmail: String?,
    inquiryUrl: String?,
    publicationStartAt: LocalDateTime?,
    publicationEndAt: LocalDateTime?,
) {
    require(companyName.isNotBlank()) { "운영 회사명은 비어 있을 수 없습니다." }
    require(title.isNotBlank()) { "부트캠프 프로그램명은 비어 있을 수 없습니다." }
    require(programType.isNotBlank()) { "프로그램 유형은 비어 있을 수 없습니다." }
    require(representativeImageUrl.isNotBlank()) { "공고 대표 이미지 URL은 비어 있을 수 없습니다." }
    require(shortDescription.isNotBlank()) { "공고 한 줄 소개는 비어 있을 수 없습니다." }
    require(content.isNotBlank()) { "부트캠프 내용은 비어 있을 수 없습니다." }
    require(capacity == null || capacity >= 0) { "모집 정원은 음수일 수 없습니다." }
    require(tuitionAmount == null || tuitionAmount >= 0) { "수강료는 음수일 수 없습니다." }
    if (recruitmentType == BootcampRecruitmentType.PERIOD) {
        require(recruitmentStartAt != null) { "기간 모집의 시작 일시는 필수입니다." }
        require(recruitmentEndAt != null) { "기간 모집의 종료 일시는 필수입니다." }
    }
    require(recruitmentType != BootcampRecruitmentType.ALWAYS_OPEN || recruitmentEndAt == null) {
        "상시 모집에는 모집 종료 일시를 둘 수 없습니다."
    }
    require(recruitmentStartAt == null || recruitmentEndAt == null || !recruitmentStartAt.isAfter(recruitmentEndAt)) {
        "모집 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
    require(!programStartDate.isAfter(programEndDate)) {
        "교육 시작일은 종료일보다 늦을 수 없습니다."
    }
    require(eligibilityAndSelectionProcess == null || eligibilityAndSelectionProcess.isNotBlank()) {
        "지원 자격 및 전형 안내는 공백일 수 없습니다."
    }
    require(logoUrl == null || logoUrl.isNotBlank()) { "운영 회사 로고 URL은 공백일 수 없습니다." }
    require(instructorInfo == null || instructorInfo.isNotBlank()) { "강사 정보는 공백일 수 없습니다." }
    require(programFeatures == null || programFeatures.isNotBlank()) { "교육 특징은 공백일 수 없습니다." }
    require(completionRequirements == null || completionRequirements.isNotBlank()) { "수료 조건은 공백일 수 없습니다." }
    when (applicationMethod) {
        ApplicationMethod.EXTERNAL_PAGE -> require(!applicationUrl.isNullOrBlank()) {
            "외부 페이지 지원 링크는 필수입니다."
        }

        ApplicationMethod.EMAIL -> require(applicationUrl == null) {
            "이메일 지원에는 외부 지원 링크를 설정할 수 없습니다."
        }
    }
    require(managerEmail == null || managerEmail.isNotBlank()) { "담당자 이메일은 공백일 수 없습니다." }
    require(inquiryUrl == null || inquiryUrl.isNotBlank()) { "문의 링크는 공백일 수 없습니다." }
    require(publicationStartAt == null || publicationEndAt == null || !publicationStartAt.isAfter(publicationEndAt)) {
        "공고 공개 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
}
