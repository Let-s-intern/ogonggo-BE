package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.implement.JobSourceHtmlSanitizer
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.implement.JobAnalysisManager
import com.ogonggo.core.job.implement.JobAnalysisReader
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceManager
import com.ogonggo.core.job.implement.JobSourceReader
import com.ogonggo.core.job.implement.TodayJobManager
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminJobService(
    private val jobReader: JobReader,
    private val jobManager: JobManager,
    private val jobMetricReader: JobMetricReader,
    private val todayJobManager: TodayJobManager,
    private val jobSourceReader: JobSourceReader,
    private val jobAnalysisReader: JobAnalysisReader,
    private val jobAnalysisManager: JobAnalysisManager,
    private val jobSourceManager: JobSourceManager,
    private val clock: Clock,
) {

    fun getJobs(
        condition: JobManagementSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
    ): AdminJobPageResult {
        val result = jobReader.readManagementPage(condition, sortType, page, size)
        return AdminJobPageResult.from(
            result = result,
            metrics = jobMetricReader.readAll(result.jobs.map { it.requiredId() }),
        )
    }

    fun getJob(jobId: Long): AdminJobResult {
        val job = jobReader.read(jobId)
        return AdminJobResult.from(
            job = job,
            metric = jobMetricReader.read(jobId),
            sourceHtml = jobSourceReader.readHtml(jobId),
            analysis = jobAnalysisReader.readForAdmin(job),
        )
    }

    /**
     * 검수 상태를 먼저 바꾸고 노출을 바꾼다. 승인은 곧 노출이므로 승인과 숨김을 함께 보내면 숨김이 남아야 한다.
     * 이미 같은 검수 상태면 다시 전이하지 않는다. 화면이 현재 값을 함께 보내도 노출이 되돌아가지 않게 하기 위해서다.
     * 공고 정보는 내용을 고친 뒤 현재 값에 덮어 한 번에 고친다. 모집 종료 일시가 바뀌면 모집 상태와 마감 알림도 다시 정해진다.
     */
    @Transactional
    fun updateJob(jobId: Long, command: AdminJobUpdateCommand) {
        val now = LocalDateTime.now(clock)
        val job = jobReader.readForUpdate(jobId)

        command.reviewStatus
            ?.takeIf { it != job.reviewStatus }
            ?.let { changeReview(job, it, now) }
        command.visibility?.let { changeVisibility(job, it) }
        if (command.title != null || command.contents.isNotEmpty()) {
            jobManager.editContent(job, JobContentEditDto(title = command.title, contents = command.contents))
        }
        if (!command.details.isEmpty()) {
            jobManager.update(job, command.details.mergeInto(job), now)
        }
        command.sourceHtml?.let { saveSourceHtml(job, it) }
    }

    /**
     * AI 서버 원문과 같은 거르기를 거쳐 저장한다. 상대 주소는 수정을 반영한 원문 URL 기준으로 바꾼다.
     * 원문은 공고 분석의 본문 해시에 들어가지 않으므로 원문을 고쳐도 분석은 그대로 보인다.
     * 걸러서 그릴 것이 남지 않으면 원문을 지우지 않고 400으로 거절한다. 원문은 지우는 경로를 두지 않았다.
     */
    private fun saveSourceHtml(job: Job, sourceHtml: String) {
        val sanitized = JobSourceHtmlSanitizer.sanitize(sourceHtml, job.sourceUrl)
            ?: throw InvalidRequestFieldException("sourceHtml", "안전한 태그만 남기면 보여 줄 내용이 없습니다.")
        jobSourceManager.save(job.requiredId(), sanitized)
    }

    /**
     * 한 트랜잭션에서 모두 바꾼다. 없거나 삭제된 공고, 승인 전 기업회원 공고의 노출처럼 하나라도 실패하면 아무것도 바꾸지 않는다.
     * 일부만 바뀌면 운영자가 어느 공고가 바뀌었는지 다시 찾아야 하기 때문이다.
     * 이미 같은 노출인 공고는 건드리지 않는다. 초안도 비노출로 보이므로 비노출 요청에 초안은 초안으로 남는다.
     */
    @Transactional
    fun changeVisibilities(command: AdminJobVisibilityChangeCommand) {
        jobReader.readAllForUpdate(command.jobIds)
            .filter { job -> job.visibility() != command.visibility }
            .forEach { job -> changeVisibility(job, command.visibility) }
    }

    /**
     * 운영자가 고친 공고 분석을 지금 본문에 대한 분석으로 저장한다. 사용자 상세에 바로 보인다.
     * 본문이 바뀌기 전까지 AI 서버는 이 분석을 덮어쓰지 못한다. 본문이 바뀌면 평소처럼 다시 분석된다.
     * AI 서버 분석 저장과 같이 공고 행을 잠가 차례로 돈다.
     */
    @Transactional
    fun replaceAnalysis(jobId: Long, content: JobAnalysisContent, adminUserId: Long) {
        jobAnalysisManager.edit(jobReader.readForUpdate(jobId), content, adminUserId, LocalDateTime.now(clock))
    }

    /** 노출·검수 상태는 바꾸지 않는다. 이미 검수 완료한 공고는 처음 확인한 시각과 관리자를 지킨다. */
    @Transactional
    fun markInspected(jobId: Long, adminUserId: Long) {
        jobManager.markInspected(jobReader.readForUpdate(jobId), adminUserId, LocalDateTime.now(clock))
    }

    /** 검수 완료가 없어도 성공한다. 마지막 동작 되돌리기가 같은 요청을 반복해도 결과가 같게 하기 위해서다. */
    @Transactional
    fun clearInspection(jobId: Long) {
        jobManager.clearInspection(jobReader.readForUpdate(jobId))
    }

    /** 반려 기록은 지우지 않는다. 반려 보관에서 "반려하고 지웠다"는 기록으로 남는다. */
    @Transactional
    fun deleteJob(jobId: Long) {
        jobManager.delete(jobReader.readForDelete(jobId), LocalDateTime.now(clock))
    }

    /** 숨긴 공고도 운영자가 알아보고 뺄 수 있도록 게시 상태와 무관하게 고른 순서대로 보여 준다. */
    fun getTodayJobs(): List<AdminTodayJobSummary> {
        val todayJobs = jobReader.readToday()
        val metrics = jobMetricReader.readAll(todayJobs.map { it.job.requiredId() })
        return todayJobs.map { todayJob ->
            AdminTodayJobSummary(
                job = AdminJobSummary.from(todayJob.job, metrics[todayJob.job.requiredId()] ?: JobMetricDto.EMPTY),
                recommendationTitle = todayJob.recommendationTitle,
                recommendationDescription = todayJob.recommendationDescription,
            )
        }
    }

    @Transactional
    fun replaceTodayJobs(todayJobs: List<TodayJobDto.Request>) {
        todayJobManager.replace(todayJobs, LocalDateTime.now(clock))
    }

    private fun Job.visibility(): AdminContentVisibility =
        AdminContentVisibility.of(publicationStatus == JobPublicationStatus.PUBLISHED)

    private fun changeVisibility(job: Job, visibility: AdminContentVisibility) {
        when (visibility) {
            AdminContentVisibility.VISIBLE -> jobManager.publish(job)
            AdminContentVisibility.HIDDEN -> jobManager.hide(job)
        }
    }

    private fun changeReview(job: Job, reviewStatus: ContentReviewStatus, now: LocalDateTime) {
        when (reviewStatus) {
            ContentReviewStatus.APPROVED -> jobManager.approveReview(job, now)
            ContentReviewStatus.PENDING -> jobManager.requestReview(job, now)
            ContentReviewStatus.REJECTED -> throw IllegalArgumentException("반려는 사유와 함께 검수 화면에서 처리합니다.")
        }
    }
}

/**
 * 보낸 값은 쓰고, 비우라고 한 값은 지우고, 나머지는 현재 값을 둔다.
 * 직군·시·도·모집 기간 유형을 바꾸면 그에 딸린 값(직무, 시·군·구, 모집 종료 일시)이 맞지 않게 되므로,
 * 딸린 값을 함께 보내지 않았다면 맞지 않는 현재 값을 지운다. 함께 보낸 값이 맞지 않으면 400이다.
 */
internal fun AdminJobDetailsPatch.mergeInto(job: Job): JobUpdateDto {
    fun <T> pick(sent: T?, field: AdminJobClearableField, current: T?): T? = when {
        sent != null -> sent
        field in cleared -> null
        else -> current
    }

    // 직무만 보내면 그 직무의 직군으로 맞춘다.
    val mergedJobField = jobField ?: jobRole?.jobField ?: pick(null, AdminJobClearableField.JOB_FIELD, job.jobField)
    val mergedJobRole = when {
        jobRole != null -> jobRole.takeIf { it.jobField == mergedJobField }
            ?: throw InvalidRequestFieldException("jobRole", "jobField와 같은 직군의 직무여야 합니다.")
        else -> pick(null, AdminJobClearableField.JOB_ROLE, job.jobRole)?.takeIf { it.jobField == mergedJobField }
    }

    val mergedRegion = region ?: subRegion?.region ?: pick(null, AdminJobClearableField.REGION, job.region)
    val mergedSubRegion = when {
        subRegion != null -> subRegion.takeIf { it.region == mergedRegion }
            ?: throw InvalidRequestFieldException("subRegion", "region과 같은 시·도의 시·군·구여야 합니다.")
        else -> pick(null, AdminJobClearableField.SUB_REGION, job.subRegion)?.takeIf { it.region == mergedRegion }
    }

    val mergedRecruitmentType = recruitmentType ?: job.recruitmentType
    val mergedRecruitmentEndAt = if (mergedRecruitmentType == JobRecruitmentType.ALWAYS_OPEN) {
        if (recruitmentEndAt != null) {
            throw InvalidRequestFieldException("recruitmentEndAt", "상시 채용에는 모집 종료 일시를 둘 수 없습니다.")
        }
        null
    } else {
        pick(recruitmentEndAt, AdminJobClearableField.RECRUITMENT_END_AT, job.recruitmentEndAt)
    }
    val mergedRecruitmentStartAt = pick(recruitmentStartAt, AdminJobClearableField.RECRUITMENT_START_AT, job.recruitmentStartAt)
    if (mergedRecruitmentStartAt != null && mergedRecruitmentEndAt != null && mergedRecruitmentStartAt.isAfter(mergedRecruitmentEndAt)) {
        throw InvalidRequestFieldException("recruitmentStartAt", "모집 종료 일시보다 늦을 수 없습니다.")
    }

    return JobUpdateDto(
        companyName = companyName ?: job.companyName,
        parentCompanyName = pick(parentCompanyName, AdminJobClearableField.PARENT_COMPANY_NAME, job.parentCompanyName),
        title = job.title,
        jobField = mergedJobField,
        jobRole = mergedJobRole,
        industry = pick(industry, AdminJobClearableField.INDUSTRY, job.industry),
        coverImageUrl = pick(coverImageUrl, AdminJobClearableField.COVER_IMAGE_URL, job.coverImageUrl),
        logoUrl = pick(logoUrl, AdminJobClearableField.LOGO_URL, job.logoUrl),
        employmentType = employmentType ?: job.employmentType,
        experienceType = experienceType ?: job.experienceType,
        experienceMinYears = pick(experienceMinYears, AdminJobClearableField.EXPERIENCE_MIN_YEARS, job.experienceMinYears),
        educationLevel = educationLevel ?: job.educationLevel,
        region = mergedRegion,
        subRegion = mergedSubRegion,
        recruitmentType = mergedRecruitmentType,
        recruitmentHeadcount = pick(recruitmentHeadcount, AdminJobClearableField.RECRUITMENT_HEADCOUNT, job.recruitmentHeadcount),
        recruitmentStartAt = mergedRecruitmentStartAt,
        recruitmentEndAt = mergedRecruitmentEndAt,
        closesWhenFilled = pick(closesWhenFilled, AdminJobClearableField.CLOSES_WHEN_FILLED, job.closesWhenFilled),
        autoCloseEnabled = pick(autoCloseEnabled, AdminJobClearableField.AUTO_CLOSE_ENABLED, job.autoCloseEnabled),
        companyAndTeamIntroduction = job.companyAndTeamIntroduction,
        responsibilities = job.responsibilities,
        qualifications = job.qualifications,
        preferredQualifications = job.preferredQualifications,
        compensation = job.compensation,
        benefits = job.benefits,
        hiringProcess = job.hiringProcess,
        recruitmentNotice = pick(recruitmentNotice, AdminJobClearableField.RECRUITMENT_NOTICE, job.recruitmentNotice),
        applicationMethod = pick(applicationMethod, AdminJobClearableField.APPLICATION_METHOD, job.applicationMethod),
        applyEmail = pick(applyEmail, AdminJobClearableField.APPLY_EMAIL, job.applyEmail),
        inquiryEmail = pick(inquiryEmail, AdminJobClearableField.INQUIRY_EMAIL, job.inquiryEmail),
        sourceUrl = pick(sourceUrl, AdminJobClearableField.SOURCE_URL, job.sourceUrl),
    )
}
