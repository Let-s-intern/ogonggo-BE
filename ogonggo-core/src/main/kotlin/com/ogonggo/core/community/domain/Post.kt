package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현재 Community에서 제공하는 게시글은 사이드 프로젝트·스터디 모집글이다.
 * 일반 글과 고민글은 아직 제품 범위에 포함하지 않는다.
 */
@Entity
@Table(name = "community_posts")
class Post internal constructor(
    authorUserId: Long,
    title: String,
    recruitmentType: RecruitmentType,
    capacity: Int,
    progressMethod: ProgressMethod,
    activityDurationMonths: Int,
    technologyStacks: List<String>,
    summary: String,
    content: String,
    eligibilityAndSelectionProcess: String?,
    recruitmentStartDate: LocalDate,
    recruitmentEndDate: LocalDate,
    positions: List<RecruitmentPosition>,
    contactMethod: ContactMethod,
    contactValue: String,
    publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
    recruitmentStatus: RecruitmentStatus = RecruitmentStatus.RECRUITING,
    closedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    init {
        require(authorUserId > 0) { "작성자 식별자는 양수여야 합니다." }
        validateEditableValues(
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
            contactValue = contactValue,
        )
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
    @Column(name = "recruitment_type", nullable = false, length = 30)
    var recruitmentType: RecruitmentType = recruitmentType
        protected set

    @Column(nullable = false)
    var capacity: Int = capacity
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_method", nullable = false, length = 20)
    var progressMethod: ProgressMethod = progressMethod
        protected set

    @Column(name = "activity_duration_months", nullable = false)
    var activityDurationMonths: Int = activityDurationMonths
        protected set

    @ElementCollection
    @CollectionTable(name = "community_post_technology_stacks", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "technology_stack", nullable = false, length = 50)
    var technologyStacks: MutableList<String> = technologyStacks.toMutableList()
        protected set

    @Column(nullable = false, length = 500)
    var summary: String = summary
        protected set

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    var content: String = content /* Lexical EditorState JSON */
        protected set

    @Column(name = "eligibility_and_selection_process", columnDefinition = "LONGTEXT")
    var eligibilityAndSelectionProcess: String? = eligibilityAndSelectionProcess
        protected set

    @Column(name = "recruitment_start_date", nullable = false)
    var recruitmentStartDate: LocalDate = recruitmentStartDate
        protected set

    @Column(name = "recruitment_end_date", nullable = false)
    var recruitmentEndDate: LocalDate = recruitmentEndDate
        protected set

    @ElementCollection
    @CollectionTable(name = "community_post_positions", joinColumns = [JoinColumn(name = "post_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    var positions: MutableList<RecruitmentPosition> = positions.toMutableList()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method", nullable = false, length = 30)
    var contactMethod: ContactMethod = contactMethod
        protected set

    @Column(name = "contact_value", nullable = false, length = 2048)
    var contactValue: String = contactValue
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
        if (recruitmentStatus == RecruitmentStatus.RECRUITING) {
            recruitmentStatus = RecruitmentStatus.CLOSED
            this.closedAt = closedAt
        }
    }

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    fun update(
        title: String,
        recruitmentType: RecruitmentType,
        capacity: Int,
        progressMethod: ProgressMethod,
        activityDurationMonths: Int,
        technologyStacks: List<String>,
        summary: String,
        content: String,
        eligibilityAndSelectionProcess: String?,
        recruitmentStartDate: LocalDate,
        recruitmentEndDate: LocalDate,
        positions: List<RecruitmentPosition>,
        contactMethod: ContactMethod,
        contactValue: String,
    ) {
        checkNotDeleted()
        validateEditableValues(
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

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 모집글은 변경할 수 없습니다." }
    }
}

private fun validateEditableValues(
    title: String,
    capacity: Int,
    activityDurationMonths: Int,
    technologyStacks: List<String>,
    summary: String,
    content: String,
    eligibilityAndSelectionProcess: String?,
    recruitmentStartDate: LocalDate,
    recruitmentEndDate: LocalDate,
    positions: List<RecruitmentPosition>,
    contactValue: String,
) {
    require(title.isNotBlank() && title.length <= 255) { "모집글 제목은 1자 이상 255자 이하여야 합니다." }
    require(capacity > 0) { "모집 인원은 1명 이상이어야 합니다." }
    require(activityDurationMonths > 0) { "활동 기간은 1개월 이상이어야 합니다." }
    require(summary.isNotBlank() && summary.length <= 500) { "한 줄 소개는 1자 이상 500자 이하여야 합니다." }
    require(content.isNotBlank()) { "모집글 본문은 비어 있을 수 없습니다." }
    require(eligibilityAndSelectionProcess == null || eligibilityAndSelectionProcess.isNotBlank()) {
        "지원 자격 및 전형은 공백일 수 없습니다."
    }
    require(!recruitmentStartDate.isAfter(recruitmentEndDate)) { "모집 시작일은 마감일보다 늦을 수 없습니다." }
    require(positions.isNotEmpty()) { "모집 포지션은 하나 이상이어야 합니다." }
    require(positions.distinct().size == positions.size) { "중복된 모집 포지션은 등록할 수 없습니다." }
    require(technologyStacks.all(String::isNotBlank)) { "기술 스택은 공백일 수 없습니다." }
    require(technologyStacks.distinct().size == technologyStacks.size) { "중복된 기술 스택은 등록할 수 없습니다." }
    require(contactValue.isNotBlank()) { "연락 방법 값은 비어 있을 수 없습니다." }
}
