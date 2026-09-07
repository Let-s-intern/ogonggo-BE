package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 목록 조회는 게시 상태와 미삭제를 항상 등가로 고정하므로 두 컬럼을 모든 인덱스의 앞에 둔다.
 * 그 뒤에는 InnoDB가 기본 키를 붙이므로 최신순 정렬이 인덱스 순서로 해결되고 LIMIT에서 조기에 끝난다.
 *
 * 필터 조합용 인덱스는 만들지 않고 필터마다 하나씩만 둔다.
 * 조합 인덱스는 필터 컬럼 뒤에 다른 필터가 끼어 식별자 정렬이 깨지므로,
 * 선택도가 높은 값에서 조건에 맞는 행을 전부 읽은 뒤에야 상위 몇 건을 고르게 된다.
 * 필터를 여러 개 지정한 조회는 그중 한 인덱스로 좁히고 나머지는 행 조건으로 거른다.
 */
@Entity
@Table(
    name = "jobs",
    indexes = [
        Index(
            name = "idx_jobs_published_latest",
            columnList = "publication_status, deleted_at",
        ),
        Index(
            name = "idx_jobs_published_employment",
            columnList = "publication_status, deleted_at, employment_type",
        ),
        Index(
            name = "idx_jobs_published_experience",
            columnList = "publication_status, deleted_at, experience_type",
        ),
        Index(
            name = "idx_jobs_owner",
            columnList = "owner_user_id, deleted_at",
        ),
    ],
)
class Job internal constructor(
    ownerUserId: Long? = null,
    companyName: String,
    parentCompanyName: String? = null,
    companyLogoUrl: String? = null,
    title: String,
    jobField: String? = null,
    coverImageUrl: String? = null,
    employmentType: EmploymentType,
    experienceType: ExperienceType,
    experienceMinYears: Int? = null,
    experienceMaxYears: Int? = null,
    educationLevel: EducationLevel = EducationLevel.ANY,
    region: String? = null,
    recruitmentType: JobRecruitmentType,
    recruitmentHeadcount: Int? = null,
    recruitmentStartAt: LocalDateTime? = null,
    recruitmentEndAt: LocalDateTime? = null,
    closesWhenFilled: Boolean? = null,
    autoCloseEnabled: Boolean? = null,
    companyAndTeamIntroduction: String? = null,
    responsibilities: String? = null,
    qualifications: String? = null,
    preferredQualifications: String? = null,
    compensation: String? = null,
    benefits: String? = null,
    hiringProcess: String? = null,
    recruitmentNotice: String? = null,
    applicationMethod: JobApplicationMethod? = null,
    sourceUrl: String? = null,
    publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
) : BaseTimeEntity() {

    init {
        require(ownerUserId == null || ownerUserId > 0) { "소유자 식별자는 양수여야 합니다." }
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            companyLogoUrl = companyLogoUrl,
            title = title,
            jobField = jobField,
            coverImageUrl = coverImageUrl,
            recruitmentHeadcount = recruitmentHeadcount,
            experienceMinYears = experienceMinYears,
            experienceMaxYears = experienceMaxYears,
            region = region,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
        )
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채용공고 식별자 */
        protected set

    /** 기업회원이 직접 등록한 공고의 소유자다. 수집한 공고는 소유자가 없어 null이다. */
    @Column(name = "owner_user_id")
    var ownerUserId: Long? = ownerUserId /* 공고를 등록한 사용자 식별자 */
        protected set

    @Column(name = "company_name", nullable = false, length = 150)
    var companyName: String = companyName /* 실제 채용 주체인 자회사명 */
        protected set

    @Column(name = "parent_company_name", length = 150)
    var parentCompanyName: String? = parentCompanyName /* 모회사명. 모회사가 없으면 null */
        protected set

    @Column(name = "company_logo_url", length = 2048)
    var companyLogoUrl: String? = companyLogoUrl /* 기업 로고 이미지 주소 */
        protected set

    @Column(nullable = false, length = 255)
    var title: String = title /* 채용공고 제목 */
        protected set

    /** 기획이 확정되기 전까지 자유 문자열로 둔다. 확정되면 고정된 값 집합으로 바꾼다. */
    @Column(name = "job_field", length = 100)
    var jobField: String? = jobField /* 직무 분야 */
        protected set

    @Column(name = "cover_image_url", length = 2048)
    var coverImageUrl: String? = coverImageUrl /* 공고 대표 이미지 주소 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    var employmentType: EmploymentType = employmentType /* 고용 형태 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 30)
    var experienceType: ExperienceType = experienceType /* 요구 경력 유형 */
        protected set

    @Column(name = "experience_min_years")
    var experienceMinYears: Int? = experienceMinYears /* 최소 요구 경력 연수 */
        protected set

    @Column(name = "experience_max_years")
    var experienceMaxYears: Int? = experienceMaxYears /* 최대 요구 경력 연수 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", nullable = false, length = 30)
    var educationLevel: EducationLevel = educationLevel /* 요구 학력 */
        protected set

    @Column(length = 100)
    var region: String? = region /* 근무 지역. 원문에 없으면 null */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_type", nullable = false, length = 20)
    var recruitmentType: JobRecruitmentType = recruitmentType /* 공고 모집 기간 유형 */
        protected set

    @Column(name = "recruitment_headcount")
    var recruitmentHeadcount: Int? = recruitmentHeadcount /* 모집 인원. 공고에 명시되지 않으면 null */
        protected set

    @Column(name = "recruitment_start_at")
    var recruitmentStartAt: LocalDateTime? = recruitmentStartAt /* 공고 모집 시작 일시 */
        protected set

    @Column(name = "recruitment_end_at")
    var recruitmentEndAt: LocalDateTime? = recruitmentEndAt /* 공고 모집 종료 일시 */
        protected set

    /** 적합한 지원자를 뽑으면 마감일 전이라도 모집을 끝내는 공고인지 나타낸다. */
    @Column(name = "closes_when_filled")
    var closesWhenFilled: Boolean? = closesWhenFilled /* 접수 시 마감 여부 */
        protected set

    /** 모집 종료 일시가 지나면 별도 조작 없이 마감으로 넘길지 나타낸다. */
    @Column(name = "auto_close_enabled")
    var autoCloseEnabled: Boolean? = autoCloseEnabled /* 자동 모집 종료 여부 */
        protected set

    @Column(name = "company_and_team_introduction", columnDefinition = "LONGTEXT")
    var companyAndTeamIntroduction: String? = companyAndTeamIntroduction /* 회사 및 팀 소개 */
        protected set

    @Column(columnDefinition = "LONGTEXT")
    var responsibilities: String? = responsibilities /* 주요 업무 */
        protected set

    @Column(columnDefinition = "LONGTEXT")
    var qualifications: String? = qualifications /* 자격 요건 */
        protected set

    @Column(name = "preferred_qualifications", columnDefinition = "LONGTEXT")
    var preferredQualifications: String? = preferredQualifications /* 우대 사항 */
        protected set

    @Column(columnDefinition = "LONGTEXT")
    var compensation: String? = compensation /* 급여 및 처우 */
        protected set

    @Column(columnDefinition = "LONGTEXT")
    var benefits: String? = benefits /* 복지 및 혜택 */
        protected set

    @Column(name = "hiring_process", columnDefinition = "LONGTEXT")
    var hiringProcess: String? = hiringProcess /* 채용 절차 */
        protected set

    @Column(name = "recruitment_notice", columnDefinition = "LONGTEXT")
    var recruitmentNotice: String? = recruitmentNotice /* 채용 안내사항 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "application_method", length = 20)
    var applicationMethod: JobApplicationMethod? = applicationMethod /* 지원 방법 */
        protected set

    @Column(name = "source_url", length = 2048)
    var sourceUrl: String? = sourceUrl /* 채용공고 원문 URL */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    var publicationStatus: JobPublicationStatus = publicationStatus /* 채용공고 게시 상태 */
        protected set

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null /* 공고 마감 처리 일시 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 공고 삭제 일시 */
        protected set

    fun update(
        companyName: String,
        parentCompanyName: String?,
        companyLogoUrl: String?,
        title: String,
        jobField: String?,
        coverImageUrl: String?,
        employmentType: EmploymentType,
        experienceType: ExperienceType,
        experienceMinYears: Int?,
        experienceMaxYears: Int?,
        educationLevel: EducationLevel,
        region: String?,
        recruitmentType: JobRecruitmentType,
        recruitmentHeadcount: Int?,
        recruitmentStartAt: LocalDateTime?,
        recruitmentEndAt: LocalDateTime?,
        closesWhenFilled: Boolean?,
        autoCloseEnabled: Boolean?,
        companyAndTeamIntroduction: String?,
        responsibilities: String?,
        qualifications: String?,
        preferredQualifications: String?,
        compensation: String?,
        benefits: String?,
        hiringProcess: String?,
        recruitmentNotice: String?,
        applicationMethod: JobApplicationMethod?,
        sourceUrl: String?,
    ) {
        checkModifiable()
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            companyLogoUrl = companyLogoUrl,
            title = title,
            jobField = jobField,
            coverImageUrl = coverImageUrl,
            recruitmentHeadcount = recruitmentHeadcount,
            experienceMinYears = experienceMinYears,
            experienceMaxYears = experienceMaxYears,
            region = region,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
        )

        this.companyName = companyName
        this.parentCompanyName = parentCompanyName
        this.companyLogoUrl = companyLogoUrl
        this.title = title
        this.jobField = jobField
        this.coverImageUrl = coverImageUrl
        this.employmentType = employmentType
        this.experienceType = experienceType
        this.experienceMinYears = experienceMinYears
        this.experienceMaxYears = experienceMaxYears
        this.educationLevel = educationLevel
        this.region = region
        this.recruitmentType = recruitmentType
        this.recruitmentHeadcount = recruitmentHeadcount
        this.recruitmentStartAt = recruitmentStartAt
        this.recruitmentEndAt = recruitmentEndAt
        this.closesWhenFilled = closesWhenFilled
        this.autoCloseEnabled = autoCloseEnabled
        this.companyAndTeamIntroduction = companyAndTeamIntroduction
        this.responsibilities = responsibilities
        this.qualifications = qualifications
        this.preferredQualifications = preferredQualifications
        this.compensation = compensation
        this.benefits = benefits
        this.hiringProcess = hiringProcess
        this.recruitmentNotice = recruitmentNotice
        this.applicationMethod = applicationMethod
        this.sourceUrl = sourceUrl
    }

    // TODO: MVP 이후 DRAFT -> PUBLISHED 등 허용 상태 전이를 명시적인 상태 머신으로 강화한다.
    fun publish() {
        checkModifiable()
        publicationStatus = JobPublicationStatus.PUBLISHED
    }

    fun hide() {
        checkModifiable()
        publicationStatus = JobPublicationStatus.HIDDEN
    }

    fun archive() {
        checkNotDeleted()
        publicationStatus = JobPublicationStatus.ARCHIVED
    }

    fun close(now: LocalDateTime) {
        checkModifiable()
        if (closedAt == null) {
            closedAt = now
        }
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    private fun checkModifiable() {
        checkNotDeleted()
        check(publicationStatus != JobPublicationStatus.ARCHIVED) { "보관된 채용공고는 변경할 수 없습니다." }
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 채용공고는 변경할 수 없습니다." }
    }
}

private fun validateJobValues(
    companyName: String,
    parentCompanyName: String?,
    companyLogoUrl: String?,
    title: String,
    jobField: String?,
    coverImageUrl: String?,
    recruitmentHeadcount: Int?,
    experienceMinYears: Int?,
    experienceMaxYears: Int?,
    region: String?,
    recruitmentStartAt: LocalDateTime?,
    recruitmentEndAt: LocalDateTime?,
) {
    require(companyName.isNotBlank()) { "회사명은 비어 있을 수 없습니다." }
    require(parentCompanyName == null || parentCompanyName.isNotBlank()) { "모회사명은 비어 있을 수 없습니다." }
    require(title.isNotBlank()) { "채용공고 제목은 비어 있을 수 없습니다." }
    require(region == null || region.isNotBlank()) { "근무 지역은 비어 있을 수 없습니다." }
    require(companyLogoUrl == null || companyLogoUrl.isNotBlank()) { "기업 로고 주소는 비어 있을 수 없습니다." }
    require(jobField == null || jobField.isNotBlank()) { "직무 분야는 비어 있을 수 없습니다." }
    require(coverImageUrl == null || coverImageUrl.isNotBlank()) { "공고 대표 이미지 주소는 비어 있을 수 없습니다." }
    require(recruitmentHeadcount == null || recruitmentHeadcount > 0) { "모집 인원은 1명 이상이어야 합니다." }
    require(experienceMinYears == null || experienceMinYears >= 0) { "최소 경력 연수는 음수일 수 없습니다." }
    require(experienceMaxYears == null || experienceMaxYears >= 0) { "최대 경력 연수는 음수일 수 없습니다." }
    require(experienceMinYears == null || experienceMaxYears == null || experienceMinYears <= experienceMaxYears) {
        "최소 경력 연수는 최대 경력 연수보다 클 수 없습니다."
    }
    require(recruitmentStartAt == null || recruitmentEndAt == null || !recruitmentStartAt.isAfter(recruitmentEndAt)) {
        "모집 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
}
