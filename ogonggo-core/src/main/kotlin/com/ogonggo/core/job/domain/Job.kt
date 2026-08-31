package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "jobs")
class Job internal constructor(
    companyName: String,
    parentCompanyName: String? = null,
    title: String,
    employmentType: EmploymentType,
    experienceType: ExperienceType,
    experienceMinYears: Int? = null,
    experienceMaxYears: Int? = null,
    educationLevel: EducationLevel = EducationLevel.ANY,
    region: String? = null,
    recruitmentType: JobRecruitmentType,
    recruitmentStartAt: LocalDateTime? = null,
    recruitmentEndAt: LocalDateTime? = null,
    companyAndTeamIntroduction: String? = null,
    responsibilities: String? = null,
    qualifications: String? = null,
    preferredQualifications: String? = null,
    compensation: String? = null,
    benefits: String? = null,
    hiringProcess: String? = null,
    sourceUrl: String? = null,
    publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
) : BaseTimeEntity() {

    init {
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            title = title,
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

    @Column(name = "company_name", nullable = false, length = 150)
    var companyName: String = companyName /* 실제 채용 주체인 자회사명 */
        protected set

    @Column(name = "parent_company_name", length = 150)
    var parentCompanyName: String? = parentCompanyName /* 모회사명. 모회사가 없으면 null */
        protected set

    @Column(nullable = false, length = 255)
    var title: String = title /* 채용공고 제목 */
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

    @Column(name = "recruitment_start_at")
    var recruitmentStartAt: LocalDateTime? = recruitmentStartAt /* 공고 모집 시작 일시 */
        protected set

    @Column(name = "recruitment_end_at")
    var recruitmentEndAt: LocalDateTime? = recruitmentEndAt /* 공고 모집 종료 일시 */
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
        title: String,
        employmentType: EmploymentType,
        experienceType: ExperienceType,
        experienceMinYears: Int?,
        experienceMaxYears: Int?,
        educationLevel: EducationLevel,
        region: String?,
        recruitmentType: JobRecruitmentType,
        recruitmentStartAt: LocalDateTime?,
        recruitmentEndAt: LocalDateTime?,
        companyAndTeamIntroduction: String?,
        responsibilities: String?,
        qualifications: String?,
        preferredQualifications: String?,
        compensation: String?,
        benefits: String?,
        hiringProcess: String?,
        sourceUrl: String?,
    ) {
        checkModifiable()
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            title = title,
            experienceMinYears = experienceMinYears,
            experienceMaxYears = experienceMaxYears,
            region = region,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
        )

        this.companyName = companyName
        this.parentCompanyName = parentCompanyName
        this.title = title
        this.employmentType = employmentType
        this.experienceType = experienceType
        this.experienceMinYears = experienceMinYears
        this.experienceMaxYears = experienceMaxYears
        this.educationLevel = educationLevel
        this.region = region
        this.recruitmentType = recruitmentType
        this.recruitmentStartAt = recruitmentStartAt
        this.recruitmentEndAt = recruitmentEndAt
        this.companyAndTeamIntroduction = companyAndTeamIntroduction
        this.responsibilities = responsibilities
        this.qualifications = qualifications
        this.preferredQualifications = preferredQualifications
        this.compensation = compensation
        this.benefits = benefits
        this.hiringProcess = hiringProcess
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
    title: String,
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
    require(experienceMinYears == null || experienceMinYears >= 0) { "최소 경력 연수는 음수일 수 없습니다." }
    require(experienceMaxYears == null || experienceMaxYears >= 0) { "최대 경력 연수는 음수일 수 없습니다." }
    require(experienceMinYears == null || experienceMaxYears == null || experienceMinYears <= experienceMaxYears) {
        "최소 경력 연수는 최대 경력 연수보다 클 수 없습니다."
    }
    require(recruitmentStartAt == null || recruitmentEndAt == null || !recruitmentStartAt.isAfter(recruitmentEndAt)) {
        "모집 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
}
