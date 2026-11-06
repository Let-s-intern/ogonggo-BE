package com.ogonggo.core.job.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import com.ogonggo.core.error.ConflictException
import com.querydsl.core.annotations.PropertyType
import com.querydsl.core.annotations.QueryType
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.contentreview.error.ContentReviewErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * 목록 조회는 게시 상태와 미삭제를 항상 등가로 고정하므로 두 컬럼을 모든 인덱스의 앞에 둔다.
 * 최신순 인덱스는 그 뒤에 목록 정렬 키를 두므로, 필터 없는 최신순이 인덱스 순서로 해결되고 LIMIT에서 조기에 끝난다.
 * 필터 인덱스 뒤에는 InnoDB가 기본 키를 붙이므로 조회수순의 식별자 보조 정렬과 관리 목록의 식별자순이 인덱스를 따른다.
 *
 * 필터 조합용 인덱스는 만들지 않고 필터마다 하나씩만 둔다.
 * 조합 인덱스는 필터 컬럼 뒤에 다른 필터가 끼어 식별자 정렬이 깨지므로,
 * 선택도가 높은 값에서 조건에 맞는 행을 전부 읽은 뒤에야 상위 몇 건을 고르게 된다.
 * 필터를 여러 개 지정한 조회는 그중 한 인덱스로 좁히고 나머지는 행 조건으로 거른다.
 * 모집 상태는 값이 둘뿐이라 인덱스로 좁혀도 남는 행이 많으므로 필터 인덱스를 두지 않고 최신순 인덱스를 읽으며 행 조건으로 거른다.
 *
 * 관리 목록은 게시 상태를 고정하지 않아 위 인덱스를 쓸 수 없으므로, 관리 필터 인덱스는 필터 컬럼과 미삭제만 둔다.
 */
@Entity
@Table(
    name = "jobs",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_jobs_source_external_id", columnNames = ["source", "external_id"]),
    ],
    indexes = [
        Index(
            name = "idx_jobs_published_list_sort",
            columnList = "publication_status, deleted_at, list_sort_key",
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
            name = "idx_jobs_published_job_field",
            columnList = "publication_status, deleted_at, job_field",
        ),
        Index(
            name = "idx_jobs_published_job_role",
            columnList = "publication_status, deleted_at, job_role",
        ),
        Index(
            name = "idx_jobs_published_industry",
            columnList = "publication_status, deleted_at, industry",
        ),
        Index(
            name = "idx_jobs_published_region",
            columnList = "publication_status, deleted_at, region",
        ),
        Index(
            name = "idx_jobs_published_sub_region",
            columnList = "publication_status, deleted_at, sub_region",
        ),
        Index(
            name = "idx_jobs_owner",
            columnList = "owner_user_id, deleted_at",
        ),
        Index(
            name = "idx_jobs_review",
            columnList = "review_status, deleted_at",
        ),
        Index(
            name = "idx_jobs_job_field",
            columnList = "job_field, deleted_at",
        ),
        Index(
            name = "idx_jobs_job_role",
            columnList = "job_role, deleted_at",
        ),
        Index(
            name = "idx_jobs_recruitment_status_end",
            columnList = "recruitment_status, recruitment_end_at",
        ),
    ],
)
class Job internal constructor(
    ownerUserId: Long? = null,
    companyName: String,
    parentCompanyName: String? = null,
    title: String,
    jobField: JobField? = null,
    jobRole: JobRole? = null,
    industry: String? = null,
    coverImageUrl: String? = null,
    logoUrl: String? = null,
    employmentType: JobEmploymentType,
    experienceType: JobExperienceType,
    experienceMinYears: Int? = null,
    educationLevel: JobEducationLevel = JobEducationLevel.ANY,
    region: Region? = null,
    subRegion: SubRegion? = null,
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
    applyEmail: String? = null,
    inquiryEmail: String? = null,
    sourceUrl: String? = null,
    publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
    source: ContentSource = ContentSource.of(ownerUserId),
    externalId: String? = null,
    listSortKey: Long,
    now: LocalDateTime,
) : BaseTimeEntity() {

    init {
        require(ownerUserId == null || ownerUserId > 0) { "소유자 식별자는 양수여야 합니다." }
        ContentSource.requireConsistent(source, ownerUserId, externalId)
        require(ownerUserId == null || publicationStatus != JobPublicationStatus.PUBLISHED) {
            "기업회원 공고는 검수 승인 전에 게시할 수 없습니다."
        }
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            title = title,
            jobField = jobField,
            jobRole = jobRole,
            industry = industry,
            coverImageUrl = coverImageUrl,
            logoUrl = logoUrl,
            applyEmail = applyEmail,
            inquiryEmail = inquiryEmail,
            recruitmentHeadcount = recruitmentHeadcount,
            experienceMinYears = experienceMinYears,
            region = region,
            subRegion = subRegion,
            recruitmentType = recruitmentType,
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

    @Column(nullable = false, length = 255)
    var title: String = title /* 채용공고 제목 */
        protected set

    /**
     * 직군·직무·산업은 기획이 확정되기 전까지 자유 문자열로 둔다. 확정되면 고정된 값 집합으로 바꾼다.
     * 세 값은 사용자 프로필의 희망 직군·직무·산업과 짝을 이룬다.
     */
    // QueryDSL APT가 JobField를 숫자 경로로 만들어 컴파일이 깨지므로 enum 경로로 고정한다.
    @field:QueryType(PropertyType.ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "job_field", length = 100)
    var jobField: JobField? = jobField /* 직군 */
        protected set

    /** 비슷한 공고 추천에서 사용자의 희망 직무와 정확히 같은지 비교한다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", length = 100)
    var jobRole: JobRole? = jobRole /* 직무 */
        protected set

    /** 비슷한 공고 추천에서 사용자의 희망 산업과 정확히 같은지 비교한다. */
    @Column(length = 100)
    var industry: String? = industry /* 산업 */
        protected set

    @Column(name = "cover_image_url", length = 2048)
    var coverImageUrl: String? = coverImageUrl /* 공고 대표 이미지 주소 */
        protected set

    /** 기업회원이 대표 이미지와 따로 올리는 로고다. 수집한 공고는 로고를 대표 이미지로 받으므로 값이 없다. */
    @Column(name = "logo_url", length = 2048)
    var logoUrl: String? = logoUrl /* 기업 로고 이미지 주소 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    var employmentType: JobEmploymentType = employmentType /* 고용 형태 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 30)
    var experienceType: JobExperienceType = experienceType /* 요구 경력 유형 */
        protected set

    @Column(name = "experience_min_years")
    var experienceMinYears: Int? = experienceMinYears /* 최소 요구 경력 연수 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", nullable = false, length = 30)
    var educationLevel: JobEducationLevel = educationLevel /* 요구 학력 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    var region: Region? = region /* 근무 시·도. 원문에 없거나 알 수 없으면 null */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_region", length = 40)
    var subRegion: SubRegion? = subRegion /* 근무 시·군·구. 시·도만 알면 null */
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

    /**
     * 지원서를 받는 이메일과 채용 문의 이메일을 따로 둔다. 한 공고에 둘 다 적힐 수 있다.
     * 한 주소로 지원과 문의를 함께 받으면 두 칸에 같은 값을 둔다.
     */
    @Column(name = "apply_email", length = 320)
    var applyEmail: String? = applyEmail /* 지원 접수 이메일 */
        protected set

    @Column(name = "inquiry_email", length = 320)
    var inquiryEmail: String? = inquiryEmail /* 채용 문의 이메일 */
        protected set

    @Column(name = "source_url", length = 2048)
    var sourceUrl: String? = sourceUrl /* 채용공고 원문 URL */
        protected set

    /** 등록 경로다. 등록할 때 정해지고 바뀌지 않는다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var source: ContentSource = source /* 등록 경로 */
        protected set

    /** 수집한 곳에서 쓰는 식별값이다. 고용24 수집분만 있으며, 같은 공고를 다시 등록하지 않는 데 쓴다. */
    @Column(name = "external_id", length = ContentSource.EXTERNAL_ID_MAX_LENGTH)
    var externalId: String? = externalId /* 외부 식별값 */
        protected set

    /**
     * 공개 목록 최신순의 정렬 키다. 규칙은 [JobListSortKey]에 있다.
     * 등록할 때 정하고, 오늘의 공고를 바꾸면 PICK 직군 비트만 벌크 갱신으로 다시 쓴다.
     * 엔티티 수정이 읽어 둔 예전 값으로 벌크 갱신을 덮어쓰지 않도록 엔티티로는 바꾸지 않는다.
     */
    @Column(name = "list_sort_key", nullable = false, updatable = false)
    var listSortKey: Long = listSortKey /* 목록 정렬 키 */
        protected set

    /**
     * 기업회원 공고 우선 노출([com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority])을 이 공고에 줄지다.
     * 전체 설정이 켜져 있고 이 값도 참일 때만 마감 전 기업회원 공고를 공개 목록 맨 앞에 둔다. 운영자가 공고마다 끈다.
     * 등록할 때 참으로 시작해 칸을 두기 전처럼 앞에 둔다. 기업회원 공고(`source = COMPANY`)에만 영향이 있고 다른 경로 공고는 값만 저장한다.
     * 정렬 키가 아니라 목록 조회 조건에서 보므로 바꾼 값은 다음 목록 요청부터 반영된다.
     */
    @Column(name = "priority_exposed", nullable = false)
    var priorityExposed: Boolean = true /* 기업회원 공고 우선 노출 여부 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    var publicationStatus: JobPublicationStatus = publicationStatus /* 채용공고 게시 상태 */
        protected set

    /**
     * 기업회원이 올린 공고만 검수하므로 등록할 때 검수 대기로 시작한다.
     * 수집한 공고는 우리가 고른 사이트에서 가져온 것이라 검수 대상이 아니어서 값이 없다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    var reviewStatus: ContentReviewStatus? = if (ownerUserId == null) null else ContentReviewStatus.PENDING /* 검수 상태 */
        protected set

    /** 운영자나 기업회원이 직접 마감한 일시다. 모집 종료 일시가 지나 자동으로 마감하면 값을 남기지 않는다. */
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null /* 공고 마감 처리 일시 */
        protected set

    /**
     * 직접 마감했거나 모집 종료 일시가 지났으면 마감이다. 등록·수정·마감할 때 다시 정하고,
     * 종료 일시가 지나는 것은 매시 자동 마감 작업(`jobAutoClose`)이 반영하므로 그 사이에는 모집 중으로 남을 수 있다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 20)
    var recruitmentStatus: JobRecruitmentStatus = recruitmentStatusAt(now) /* 모집 상태 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 공고 삭제 일시 */
        protected set

    /**
     * 운영자가 게시된 공고를 직접 훑어보고 확인했다는 기록이다(검수 완료). 검수 상태([reviewStatus])는 기업회원 공고의
     * 게시 전 승인이라 다른 개념이다. 노출과 무관하며, 운영자가 내용을 고쳐도 지우지 않는다. "누가 언제 봤다"는 기록이기 때문이다.
     * AI 서버가 다시 수집해 본문이나 원문이 바뀌면 운영자가 본 내용이 아니므로 지운다.
     * 검수 완료·해제는 공고 내용이 아니라 수정 일시([updatedAt])를 바꾸지 않도록 `JobManager`의 대상 UPDATE로만 쓴다.
     */
    @Column(name = "inspected_at")
    var inspectedAt: LocalDateTime? = null /* 검수 완료 일시 */
        protected set

    @Column(name = "inspected_by_user_id")
    var inspectedByUserId: Long? = null /* 검수 완료한 관리자 식별자 */
        protected set

    /**
     * 기업회원 공고가 처음 검수 승인되어 게시된 일시다. 다시 승인해도 바꾸지 않는다. 수집 공고와 이 칸을 두기 전에 승인한 공고는 null이다.
     * 기업회원 공고는 승인되어야 사용자에게 보이므로, 사용자에게 보이는 등록 일시([publicRegisteredAt])로 쓴다.
     */
    @Column(name = "first_approved_at")
    var firstApprovedAt: LocalDateTime? = null /* 처음 검수 승인 일시 */
        protected set

    /**
     * 운영자가 관리자 콘솔에서 공고 내용(제목·본문·공고 정보·원문)을 마지막으로 고친 일시와 관리자다.
     * 운영자 수정은 크롤링·AI 값을 바로잡는 마지막 방어선이라, 값이 있는 동안 AI 서버의 다시 수집은 공고 내용과 원문을 바꾸지 않는다.
     * 자동으로 풀리지 않는다. 원문 페이지가 바뀌었는지 서버가 알 수 없고, 운영자가 고친 내용을 조용히 덮는 것보다
     * 다시 수집이 반영되지 않는 쪽이 덜 위험하기 때문이다.
     */
    @Column(name = "content_edited_at")
    var contentEditedAt: LocalDateTime? = null /* 운영자가 공고 내용을 마지막으로 고친 일시 */
        protected set

    @Column(name = "content_edited_by_user_id")
    var contentEditedByUserId: Long? = null /* 공고 내용을 마지막으로 고친 관리자 식별자 */
        protected set

    /**
     * 운영자가 기업회원 대신 등록한 공고면 등록한 관리자와 일시다(대리 등록). 기업회원이 직접 등록했거나 수집한 공고는 null이다.
     * 등록할 때 한 번만 남기며 기업회원이 나중에 고쳐도 지우지 않는다. "누가 대신 올렸다"는 기록이기 때문이다.
     */
    @Column(name = "proxy_registered_at")
    var proxyRegisteredAt: LocalDateTime? = null /* 대리 등록 일시 */
        protected set

    @Column(name = "proxy_registered_by_user_id")
    var proxyRegisteredByUserId: Long? = null /* 대리 등록한 관리자 식별자 */
        protected set

    fun update(
        companyName: String,
        parentCompanyName: String?,
        title: String,
        jobField: JobField?,
        jobRole: JobRole?,
        industry: String?,
        coverImageUrl: String?,
        logoUrl: String?,
        employmentType: JobEmploymentType,
        experienceType: JobExperienceType,
        experienceMinYears: Int?,
        educationLevel: JobEducationLevel,
        region: Region?,
        subRegion: SubRegion?,
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
        applyEmail: String?,
        inquiryEmail: String?,
        sourceUrl: String?,
        now: LocalDateTime,
    ) {
        checkNotDeleted()
        validateJobValues(
            companyName = companyName,
            parentCompanyName = parentCompanyName,
            title = title,
            jobField = jobField,
            jobRole = jobRole,
            industry = industry,
            coverImageUrl = coverImageUrl,
            logoUrl = logoUrl,
            applyEmail = applyEmail,
            inquiryEmail = inquiryEmail,
            recruitmentHeadcount = recruitmentHeadcount,
            experienceMinYears = experienceMinYears,
            region = region,
            subRegion = subRegion,
            recruitmentType = recruitmentType,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
        )

        this.companyName = companyName
        this.parentCompanyName = parentCompanyName
        this.title = title
        this.jobField = jobField
        this.jobRole = jobRole
        this.industry = industry
        this.coverImageUrl = coverImageUrl
        this.logoUrl = logoUrl
        this.employmentType = employmentType
        this.experienceType = experienceType
        this.experienceMinYears = experienceMinYears
        this.educationLevel = educationLevel
        this.region = region
        this.subRegion = subRegion
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
        this.applyEmail = applyEmail
        this.inquiryEmail = inquiryEmail
        this.sourceUrl = sourceUrl
        this.recruitmentStatus = recruitmentStatusAt(now)
    }

    /**
     * 제목과 본문 칸 중 넘어온 것만 바꾼다. 본문 값이 null이면 그 칸을 비운다.
     * 수집한 공고도 고칠 수 있다. 크롤러가 원문 구조를 잘못 읽어 왔을 때 통째로 내리지 않게 하기 위해서다.
     */
    /** 운영자가 공고 내용을 고쳤다. 마지막으로 고친 일시와 관리자를 남긴다. */
    fun markContentEdited(userId: Long, now: LocalDateTime) {
        checkNotDeleted()
        require(userId > 0) { "관리자 식별자는 양수여야 합니다." }
        contentEditedAt = now
        contentEditedByUserId = userId
    }

    /** 운영자가 기업회원 대신 등록했다고 남긴다. 기업회원 공고에만, 한 번만 남긴다. */
    fun markProxyRegistered(adminUserId: Long, now: LocalDateTime) {
        checkNotDeleted()
        require(adminUserId > 0) { "관리자 식별자는 양수여야 합니다." }
        check(ownerUserId != null) { "기업회원 공고만 대리 등록으로 남깁니다." }
        check(proxyRegisteredAt == null) { "이미 대리 등록으로 남긴 공고입니다." }
        proxyRegisteredAt = now
        proxyRegisteredByUserId = adminUserId
    }

    /** 운영자가 고친 내용이 있는지. 참이면 AI 서버의 다시 수집이 공고 내용과 원문을 바꾸지 않는다. */
    fun isContentEditedByAdmin(): Boolean = contentEditedAt != null

    fun editContent(title: String?, contents: Map<JobContentField, String?>) {
        checkNotDeleted()
        require(title == null || title.isNotBlank()) { "채용공고 제목은 비어 있을 수 없습니다." }
        require(contents.values.all { it == null || it.isNotBlank() }) { "본문 칸은 공백일 수 없습니다." }

        title?.let { this.title = it }
        contents.forEach { (field, value) ->
            when (field) {
                JobContentField.COMPANY_AND_TEAM_INTRODUCTION -> companyAndTeamIntroduction = value
                JobContentField.RESPONSIBILITIES -> responsibilities = value
                JobContentField.QUALIFICATIONS -> qualifications = value
                JobContentField.PREFERRED_QUALIFICATIONS -> preferredQualifications = value
                JobContentField.COMPENSATION -> compensation = value
                JobContentField.BENEFITS -> benefits = value
                JobContentField.HIRING_PROCESS -> hiringProcess = value
            }
        }
    }

    fun contentOf(field: JobContentField): String? = when (field) {
        JobContentField.COMPANY_AND_TEAM_INTRODUCTION -> companyAndTeamIntroduction
        JobContentField.RESPONSIBILITIES -> responsibilities
        JobContentField.QUALIFICATIONS -> qualifications
        JobContentField.PREFERRED_QUALIFICATIONS -> preferredQualifications
        JobContentField.COMPENSATION -> compensation
        JobContentField.BENEFITS -> benefits
        JobContentField.HIRING_PROCESS -> hiringProcess
    }

    /**
     * 공고 분석([JobAnalysis])이 어느 본문에 대한 것인지 가르는 해시다. 제목과 본문 여덟 칸으로 센다.
     * 사용자가 원문 탭에서 보는 글이 바뀌면 분석도 다시 만들어야 하므로, 그 밖의 칸(마감·노출 등)은 넣지 않는다.
     */
    fun contentHash(): String {
        val text = listOf(
            title,
            companyAndTeamIntroduction,
            responsibilities,
            qualifications,
            preferredQualifications,
            compensation,
            benefits,
            hiringProcess,
            recruitmentNotice,
        ).joinToString(CONTENT_HASH_SEPARATOR) { it.orEmpty() }
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** 모집 종료 일시와 같은 시각까지는 모집 중으로 본다. 자동 마감 작업과 경계를 맞춘다. */
    private fun recruitmentStatusAt(now: LocalDateTime): JobRecruitmentStatus {
        val expired = recruitmentEndAt?.isBefore(now) == true
        return if (closedAt != null || expired) JobRecruitmentStatus.CLOSED else JobRecruitmentStatus.RECRUITING
    }

    // TODO: MVP 이후 DRAFT -> PUBLISHED 등 허용 상태 전이를 명시적인 상태 머신으로 강화한다.
    /** 기업회원 공고는 검수 승인을 받아야만 노출한다. 게시하는 쪽이 누구든 같은 규칙을 따른다. */
    fun publish() {
        checkNotDeleted()
        if (reviewStatus != null && reviewStatus != ContentReviewStatus.APPROVED) {
            throw ConflictException(ContentReviewErrorCode.REVIEW_NOT_APPROVED)
        }
        publicationStatus = JobPublicationStatus.PUBLISHED
    }

    fun hide() {
        checkNotDeleted()
        publicationStatus = JobPublicationStatus.HIDDEN
    }

    fun changePriorityExposed(priorityExposed: Boolean) {
        checkNotDeleted()
        this.priorityExposed = priorityExposed
    }

    fun close(now: LocalDateTime) {
        checkNotDeleted()
        if (closedAt == null) {
            closedAt = now
        }
        recruitmentStatus = JobRecruitmentStatus.CLOSED
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    /** 승인하면 곧바로 노출한다. 승인 결과를 알릴 경로가 없어 다시 게시하게 하면 공고가 비노출로 남는다. */
    /**
     * 사용자에게 보이는 등록 일시다. 기업회원 공고는 승인 전에 보이지 않으므로 처음 승인한 일시이고, 그 밖에는 등록 일시([createdAt])다.
     * 수집 공고는 등록과 함께 게시된다. 관리자 화면의 등록 일시는 오공고에 들어온 시각이라 늘 [createdAt]이다.
     */
    fun publicRegisteredAt(): LocalDateTime = firstApprovedAt ?: createdAt

    fun approveReview(now: LocalDateTime) {
        checkReviewable()
        if (firstApprovedAt == null) {
            firstApprovedAt = now
        }
        reviewStatus = ContentReviewStatus.APPROVED
        publicationStatus = JobPublicationStatus.PUBLISHED
    }

    fun rejectReview() {
        checkReviewable()
        reviewStatus = ContentReviewStatus.REJECTED
        unpublish()
    }

    /** 기업회원이 내용을 고치거나 운영자가 판정을 되돌리면 다시 검수를 기다리며, 그동안 노출하지 않는다. */
    fun requestReview() {
        checkReviewable()
        reviewStatus = ContentReviewStatus.PENDING
        unpublish()
    }

    private fun unpublish() {
        if (publicationStatus == JobPublicationStatus.PUBLISHED) {
            publicationStatus = JobPublicationStatus.HIDDEN
        }
    }

    private fun checkReviewable() {
        checkNotDeleted()
        if (reviewStatus == null) {
            throw ConflictException(ContentReviewErrorCode.CONTENT_NOT_REVIEWABLE)
        }
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 채용공고는 변경할 수 없습니다." }
    }
}

private fun validateJobValues(
    companyName: String,
    parentCompanyName: String?,
    title: String,
    jobField: JobField?,
    jobRole: JobRole?,
    industry: String?,
    coverImageUrl: String?,
    logoUrl: String?,
    applyEmail: String?,
    inquiryEmail: String?,
    recruitmentHeadcount: Int?,
    experienceMinYears: Int?,
    region: Region?,
    subRegion: SubRegion?,
    recruitmentType: JobRecruitmentType,
    recruitmentStartAt: LocalDateTime?,
    recruitmentEndAt: LocalDateTime?,
) {
    require(companyName.isNotBlank()) { "회사명은 비어 있을 수 없습니다." }
    require(parentCompanyName == null || parentCompanyName.isNotBlank()) { "모회사명은 비어 있을 수 없습니다." }
    require(title.isNotBlank()) { "채용공고 제목은 비어 있을 수 없습니다." }
    require(subRegion == null || subRegion.region == region) { "시·군·구는 같은 시·도에 속해야 합니다." }
    require(jobRole == null || jobRole.jobField == jobField) { "직무는 같은 직군에 속해야 합니다." }
    require(industry == null || industry.isNotBlank()) { "산업은 비어 있을 수 없습니다." }
    require(coverImageUrl == null || coverImageUrl.isNotBlank()) { "공고 대표 이미지 주소는 비어 있을 수 없습니다." }
    require(logoUrl == null || logoUrl.isNotBlank()) { "기업 로고 주소는 비어 있을 수 없습니다." }
    require(applyEmail == null || applyEmail.isNotBlank()) { "지원 접수 이메일은 비어 있을 수 없습니다." }
    require(inquiryEmail == null || inquiryEmail.isNotBlank()) { "채용 문의 이메일은 비어 있을 수 없습니다." }
    require(recruitmentHeadcount == null || recruitmentHeadcount > 0) { "모집 인원은 1명 이상이어야 합니다." }
    require(experienceMinYears == null || experienceMinYears >= 0) { "최소 경력 연수는 음수일 수 없습니다." }
    require(recruitmentType != JobRecruitmentType.ALWAYS_OPEN || recruitmentEndAt == null) {
        "상시 채용에는 모집 종료 일시를 둘 수 없습니다."
    }
    require(recruitmentStartAt == null || recruitmentEndAt == null || !recruitmentStartAt.isAfter(recruitmentEndAt)) {
        "모집 시작 일시는 종료 일시보다 늦을 수 없습니다."
    }
}

/** 칸 사이 구분자. 본문에 나오지 않는 글자를 써서 칸의 경계가 옮겨 가도 같은 해시가 되지 않게 한다. */
private const val CONTENT_HASH_SEPARATOR = "\u0000"
