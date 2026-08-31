package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.error.ConflictException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "bootcamps")
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
    applicationMethod: ApplicationMethod,
    applicationUrl: String? = null,
    managerEmail: String? = null,
    inquiryUrl: String? = null,
    publicationStartAt: LocalDateTime? = null,
    publicationEndAt: LocalDateTime? = null,
    sourceUrl: String? = null,
    status: BootcampStatus = BootcampStatus.DRAFT,
    closedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    init {
        require(ownerUserId == null || ownerUserId > 0) { "소유자 식별자는 양수여야 합니다." }
        require((status == BootcampStatus.CLOSED) == (closedAt != null)) {
            "모집 마감 상태와 마감 일시가 일치해야 합니다."
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
        this.applicationMethod = applicationMethod
        this.applicationUrl = applicationUrl
        this.managerEmail = managerEmail
        this.inquiryUrl = inquiryUrl
        this.publicationStartAt = publicationStartAt
        this.publicationEndAt = publicationEndAt
        this.sourceUrl = sourceUrl
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

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
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
    require(recruitmentStartAt == null || recruitmentEndAt == null || !recruitmentStartAt.isAfter(recruitmentEndAt)) {
        "모집 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
    require(!programStartDate.isAfter(programEndDate)) {
        "교육 시작일은 종료일보다 늦을 수 없습니다."
    }
    require(eligibilityAndSelectionProcess == null || eligibilityAndSelectionProcess.isNotBlank()) {
        "지원 자격 및 전형 안내는 공백일 수 없습니다."
    }
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
