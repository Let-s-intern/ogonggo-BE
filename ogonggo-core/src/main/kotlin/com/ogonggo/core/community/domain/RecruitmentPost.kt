package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.editor.lexical.LexicalEditorStateJson
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현재 Community에서 제공하는 게시글은 사이드 프로젝트·스터디 모집글이다.
 * 일반 글과 고민글은 아직 제품 범위에 포함하지 않는다.
 */
@Entity
@Table(name = "community_posts")
class RecruitmentPost internal constructor(
    authorUserId: Long,
    title: String,
    recruitmentType: RecruitmentType?,
    capacity: Int?,
    progressMethod: ProgressMethod?,
    activityDurationMonths: Int?,
    technologyStacks: List<String>,
    summary: String?,
    content: String?,
    eligibilityAndSelectionProcess: String?,
    recruitmentStartDate: LocalDate?,
    recruitmentEndDate: LocalDate?,
    positions: List<RecruitmentPosition>,
    contactMethod: ContactMethod?,
    contactValue: String?,
    publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
    recruitmentStatus: RecruitmentStatus = RecruitmentStatus.RECRUITING,
    closedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    init {
        require(authorUserId > 0) { "작성자 식별자는 양수여야 합니다." }
        if (publicationStatus != PublicationStatus.DRAFT) {
            validatePublishedValues(
                title = title,
                recruitmentType = recruitmentType,
                capacity = capacity,
                progressMethod = progressMethod,
                activityDurationMonths = activityDurationMonths,
                technologyStacks = technologyStacks,
                summary = summary,
                content = content,
                eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
                recruitmentStartDate = recruitmentStartDate,
                recruitmentEndDate = recruitmentEndDate,
                positions = positions,
                contactMethod = contactMethod,
                contactValue = contactValue,
            )
        } else {
            validateDraftValues(
                title = title,
                capacity = capacity,
                activityDurationMonths = activityDurationMonths,
                technologyStacks = technologyStacks,
                summary = summary,
                content = content,
                eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
                recruitmentStartDate = recruitmentStartDate,
                recruitmentEndDate = recruitmentEndDate,
                positions = positions,
                contactMethod = contactMethod,
                contactValue = contactValue,
            )
        }
        require((recruitmentStatus == RecruitmentStatus.CLOSED) == (closedAt != null)) {
            "모집 상태와 마감 일시가 일치해야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "author_user_id", nullable = false)
    var authorUserId: Long = authorUserId
        protected set

    @Column(nullable = false, length = 255)
    var title: String = title
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_type", length = 30)
    var recruitmentType: RecruitmentType? = recruitmentType
        protected set

    @Column
    var capacity: Int? = capacity
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_method", length = 20)
    var progressMethod: ProgressMethod? = progressMethod
        protected set

    @Column(name = "activity_duration_months")
    var activityDurationMonths: Int? = activityDurationMonths
        protected set

    @ElementCollection
    @CollectionTable(name = "community_post_technology_stacks", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "technology_stack", nullable = false, length = 50)
    var technologyStacks: MutableList<String> = technologyStacks.toMutableList()
        get() = field.toMutableList()
        protected set

    @Column(length = 500)
    var summary: String? = summary
        protected set

    @Column(columnDefinition = "LONGTEXT")
    var content: String? = content /* Lexical EditorState JSON */
        protected set

    @Column(name = "eligibility_and_selection_process", columnDefinition = "LONGTEXT")
    var eligibilityAndSelectionProcess: String? = eligibilityAndSelectionProcess
        protected set

    @Column(name = "recruitment_start_date")
    var recruitmentStartDate: LocalDate? = recruitmentStartDate
        protected set

    @Column(name = "recruitment_end_date")
    var recruitmentEndDate: LocalDate? = recruitmentEndDate
        protected set

    @ElementCollection
    @CollectionTable(name = "community_post_positions", joinColumns = [JoinColumn(name = "post_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    var positions: MutableList<RecruitmentPosition> = positions.toMutableList()
        get() = field.toMutableList()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method", length = 30)
    var contactMethod: ContactMethod? = contactMethod
        protected set

    @Column(name = "contact_value", length = 2048)
    var contactValue: String? = contactValue
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    var publicationStatus: PublicationStatus = publicationStatus
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 20)
    var recruitmentStatus: RecruitmentStatus = recruitmentStatus
        protected set

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = closedAt
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun close(closedAt: LocalDateTime) {
        checkNotDeleted()
        check(publicationStatus != PublicationStatus.DRAFT) { "임시저장 모집글은 마감할 수 없습니다." }
        if (recruitmentStatus == RecruitmentStatus.RECRUITING) {
            recruitmentStatus = RecruitmentStatus.CLOSED
            this.closedAt = closedAt
        }
    }

    fun reopen() {
        checkNotDeleted()
        check(publicationStatus != PublicationStatus.DRAFT) { "임시저장 모집글은 재모집할 수 없습니다." }
        if (recruitmentStatus == RecruitmentStatus.CLOSED) {
            recruitmentStatus = RecruitmentStatus.RECRUITING
            closedAt = null
        }
    }

    fun publish() {
        checkNotDeleted()
        if (publicationStatus == PublicationStatus.PUBLISHED) return
        check(publicationStatus == PublicationStatus.DRAFT) { "임시저장 모집글만 게시할 수 있습니다." }
        validatePublishedValues(
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks,
            summary = summary,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions,
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
        publicationStatus = PublicationStatus.PUBLISHED
    }

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    fun copyAsDraft(): RecruitmentPost = RecruitmentPost(
        authorUserId = authorUserId,
        title = title,
        recruitmentType = recruitmentType,
        capacity = capacity,
        progressMethod = progressMethod,
        activityDurationMonths = activityDurationMonths,
        technologyStacks = technologyStacks,
        summary = summary,
        content = content,
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
        publicationStatus = PublicationStatus.DRAFT,
        recruitmentStatus = RecruitmentStatus.RECRUITING,
        closedAt = null,
    )

    fun replaceDraftContent(content: String?) {
        check(publicationStatus == PublicationStatus.DRAFT) { "임시저장 모집글만 본문을 교체할 수 있습니다." }
        this.content = content
    }

    fun updateDraft(
        title: String,
        recruitmentType: RecruitmentType?,
        capacity: Int?,
        progressMethod: ProgressMethod?,
        activityDurationMonths: Int?,
        technologyStacks: List<String>,
        summary: String?,
        content: String?,
        eligibilityAndSelectionProcess: String?,
        recruitmentStartDate: LocalDate?,
        recruitmentEndDate: LocalDate?,
        positions: List<RecruitmentPosition>,
        contactMethod: ContactMethod?,
        contactValue: String?,
    ) {
        checkNotDeleted()
        check(publicationStatus == PublicationStatus.DRAFT) { "임시저장 모집글만 수정할 수 있습니다." }
        validateDraftValues(
            title = title,
            capacity = capacity,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks,
            summary = summary,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions,
            contactMethod = contactMethod,
            contactValue = contactValue,
        )

        this.title = title
        this.recruitmentType = recruitmentType
        this.capacity = capacity
        this.progressMethod = progressMethod
        this.activityDurationMonths = activityDurationMonths
        this.technologyStacks = technologyStacks.toMutableList()
        this.summary = summary
        this.content = content
        this.eligibilityAndSelectionProcess = eligibilityAndSelectionProcess
        this.recruitmentStartDate = recruitmentStartDate
        this.recruitmentEndDate = recruitmentEndDate
        this.positions = positions.toMutableList()
        this.contactMethod = contactMethod
        this.contactValue = contactValue
    }

    fun update(
        title: String,
        recruitmentType: RecruitmentType?,
        capacity: Int?,
        progressMethod: ProgressMethod?,
        activityDurationMonths: Int?,
        technologyStacks: List<String>,
        summary: String?,
        content: String?,
        eligibilityAndSelectionProcess: String?,
        recruitmentStartDate: LocalDate?,
        recruitmentEndDate: LocalDate?,
        positions: List<RecruitmentPosition>,
        contactMethod: ContactMethod?,
        contactValue: String?,
    ) {
        checkNotDeleted()
        validatePublishedValues(
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks,
            summary = summary,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions,
            contactMethod = contactMethod,
            contactValue = contactValue,
        )

        this.title = title
        this.recruitmentType = checkNotNull(recruitmentType)
        this.capacity = checkNotNull(capacity)
        this.progressMethod = checkNotNull(progressMethod)
        this.activityDurationMonths = checkNotNull(activityDurationMonths)
        this.technologyStacks = technologyStacks.toMutableList()
        this.summary = checkNotNull(summary)
        this.content = checkNotNull(content)
        this.eligibilityAndSelectionProcess = eligibilityAndSelectionProcess
        this.recruitmentStartDate = checkNotNull(recruitmentStartDate)
        this.recruitmentEndDate = checkNotNull(recruitmentEndDate)
        this.positions = positions.toMutableList()
        this.contactMethod = checkNotNull(contactMethod)
        this.contactValue = checkNotNull(contactValue)
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 모집글은 변경할 수 없습니다." }
    }
}

private fun validatePublishedValues(
    title: String,
    recruitmentType: RecruitmentType?,
    capacity: Int?,
    progressMethod: ProgressMethod?,
    activityDurationMonths: Int?,
    technologyStacks: List<String>,
    summary: String?,
    content: String?,
    eligibilityAndSelectionProcess: String?,
    recruitmentStartDate: LocalDate?,
    recruitmentEndDate: LocalDate?,
    positions: List<RecruitmentPosition>,
    contactMethod: ContactMethod?,
    contactValue: String?,
) {
    require(title.isNotBlank() && title.length <= 255) { "모집글 제목은 1자 이상 255자 이하여야 합니다." }
    requireNotNull(recruitmentType) { "모집 구분은 필수입니다." }
    requireNotNull(capacity) { "모집 인원은 필수입니다." }
    require(capacity > 0) { "모집 인원은 1명 이상이어야 합니다." }
    requireNotNull(progressMethod) { "진행 방식은 필수입니다." }
    requireNotNull(activityDurationMonths) { "활동 기간은 필수입니다." }
    require(activityDurationMonths > 0) { "활동 기간은 1개월 이상이어야 합니다." }
    requireNotNull(summary) { "한 줄 소개는 필수입니다." }
    require(summary.isNotBlank() && summary.length <= 500) { "한 줄 소개는 1자 이상 500자 이하여야 합니다." }
    requireNotNull(content) { "모집글 본문은 필수입니다." }
    require(LexicalEditorStateJson.isValid(content)) { "모집글 본문은 올바른 에디터 JSON이어야 합니다." }
    require(eligibilityAndSelectionProcess == null || eligibilityAndSelectionProcess.isNotBlank()) {
        "지원 자격 및 전형은 공백일 수 없습니다."
    }
    requireNotNull(recruitmentStartDate) { "모집 시작일은 필수입니다." }
    requireNotNull(recruitmentEndDate) { "모집 종료일은 필수입니다." }
    require(!recruitmentStartDate.isAfter(recruitmentEndDate)) { "모집 시작일은 마감일보다 늦을 수 없습니다." }
    require(positions.isNotEmpty()) { "모집 포지션은 하나 이상이어야 합니다." }
    require(positions.distinct().size == positions.size) { "중복된 모집 포지션은 등록할 수 없습니다." }
    require(technologyStacks.all(String::isNotBlank)) { "기술 스택은 공백일 수 없습니다." }
    require(technologyStacks.distinct().size == technologyStacks.size) { "중복된 기술 스택은 등록할 수 없습니다." }
    require(technologyStacks.size <= TECHNOLOGY_STACK_MAX_COUNT) { "기술 스택은 20개 이하로 입력해야 합니다." }
    require(technologyStacks.all { it.length <= TECHNOLOGY_STACK_MAX_LENGTH }) {
        "기술 스택은 50자 이하여야 합니다."
    }
    requireNotNull(contactMethod) { "연락 방법은 필수입니다." }
    requireNotNull(contactValue) { "연락 방법 값은 필수입니다." }
    require(contactValue.isNotBlank()) { "연락 방법 값은 비어 있을 수 없습니다." }
    when (contactMethod) {
        ContactMethod.EMAIL -> require(EMAIL_PATTERN.matches(contactValue)) {
            "이메일 형식으로 입력해 주세요."
        }
        ContactMethod.OPEN_KAKAO -> require(isHttpUrl(contactValue)) {
            "카카오톡 오픈채팅 링크를 입력해 주세요."
        }
    }
}

private fun validateDraftValues(
    title: String,
    capacity: Int?,
    activityDurationMonths: Int?,
    technologyStacks: List<String>,
    summary: String?,
    content: String?,
    eligibilityAndSelectionProcess: String?,
    recruitmentStartDate: LocalDate?,
    recruitmentEndDate: LocalDate?,
    positions: List<RecruitmentPosition>,
    contactMethod: ContactMethod?,
    contactValue: String?,
) {
    require(title.isNotBlank() && title.length <= 255) { "모집글 제목은 1자 이상 255자 이하여야 합니다." }
    require(capacity == null || capacity > 0) { "모집 인원은 1명 이상이어야 합니다." }
    require(activityDurationMonths == null || activityDurationMonths > 0) { "활동 기간은 1개월 이상이어야 합니다." }
    require(summary == null || (summary.isNotBlank() && summary.length <= 500)) {
        "한 줄 소개는 1자 이상 500자 이하여야 합니다."
    }
    require(content == null || LexicalEditorStateJson.isValid(content)) {
        "모집글 본문은 올바른 에디터 JSON이어야 합니다."
    }
    require(eligibilityAndSelectionProcess == null || eligibilityAndSelectionProcess.isNotBlank()) {
        "지원 자격 및 전형은 공백일 수 없습니다."
    }
    if (recruitmentStartDate != null && recruitmentEndDate != null) {
        require(!recruitmentStartDate.isAfter(recruitmentEndDate)) { "모집 시작일은 마감일보다 늦을 수 없습니다." }
    }
    require(positions.distinct().size == positions.size) { "중복된 모집 포지션은 등록할 수 없습니다." }
    require(technologyStacks.all(String::isNotBlank)) { "기술 스택은 공백일 수 없습니다." }
    require(technologyStacks.distinct().size == technologyStacks.size) { "중복된 기술 스택은 등록할 수 없습니다." }
    require(technologyStacks.size <= TECHNOLOGY_STACK_MAX_COUNT) { "기술 스택은 20개 이하로 입력해야 합니다." }
    require(technologyStacks.all { it.length <= TECHNOLOGY_STACK_MAX_LENGTH }) {
        "기술 스택은 50자 이하여야 합니다."
    }
    require(contactValue == null || contactValue.isNotBlank()) { "연락 방법 값은 비어 있을 수 없습니다." }
    if (contactMethod != null && contactValue != null) {
        when (contactMethod) {
            ContactMethod.EMAIL -> require(EMAIL_PATTERN.matches(contactValue)) {
                "이메일 형식으로 입력해 주세요."
            }
            ContactMethod.OPEN_KAKAO -> require(isHttpUrl(contactValue)) {
                "카카오톡 오픈채팅 링크를 입력해 주세요."
            }
        }
    }
}

private fun isHttpUrl(value: String): Boolean = runCatching {
    URI(value).let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
}.getOrDefault(false)

private const val TECHNOLOGY_STACK_MAX_COUNT = 20
private const val TECHNOLOGY_STACK_MAX_LENGTH = 50
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
