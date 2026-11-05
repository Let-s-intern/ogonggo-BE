package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.job.implement.JobSourceHtmlSanitizer
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceManager
import com.ogonggo.core.job.implement.JobSourceReader
import com.ogonggo.core.job.implement.JobTagAppender
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiJobService(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobTagAppender: JobTagAppender,
    private val jobSourceManager: JobSourceManager,
    private val jobSourceReader: JobSourceReader,
    private val clock: Clock,
) {

    /**
     * AI 서버가 수집한 공고를 곧바로 게시한다. 검수는 기업회원이 올린 공고만 거친다.
     *
     * 같은 원문을 다시 등록하면 거절하고, AI 서버는 등록 응답의 식별자로 공고를 교체한다.
     */
    @Transactional
    fun register(command: AiJobRegistrationCommand): Long {
        if (jobReader.existsBySourceUrl(command.job.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        val job = jobAppender.append(command.job.toAppendDto())
        val jobId = checkNotNull(job.id) { "저장된 채용공고 식별자가 없습니다." }
        jobTagAppender.append(jobId, command.tags)
        saveSource(job, command.job.sourceHtml, command.job.sourceUrl)
        return jobId
    }

    /**
     * 다시 수집·분류한 값으로 공고 전체를 바꾼다. 게시 상태는 바꾸지 않는다.
     *
     * 운영자가 관리자 콘솔에서 내용을 고친 공고([Job.isContentEditedByAdmin])는 공고 내용과 원문을 그대로 두고 성공한다.
     * 보내는 칸이 모두 운영자가 고칠 수 있는 칸이라 운영자가 고친 칸만 골라 낼 수 없고, 고친 내용을 조용히 덮으면 안 되기 때문이다.
     * 마감·숨김·삭제는 각자의 경로가 있어 이 보호와 무관하다.
     *
     * 검수 완료한 공고의 본문이나 원문이 바뀌면 운영자가 본 내용이 아니므로 검수 완료를 지워 다시 미검수로 보이게 한다.
     */
    @Transactional
    fun replace(jobId: Long, command: AiJobCommand) {
        val job = jobReader.readCrawledForUpdate(jobId)
        if (job.isContentEditedByAdmin()) {
            return
        }
        if (command.sourceUrl != job.sourceUrl && jobReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        val previousHash = job.contentHash()
        jobManager.update(job, command.toUpdateDto(), LocalDateTime.now(clock))
        val sourceChanged = saveSource(job, command.sourceHtml, command.sourceUrl)
        if (previousHash != job.contentHash() || sourceChanged) {
            clearInspectionOf(job)
        }
    }

    /**
     * 원문만 바꾼다. 원문 저장 전에 보낸 공고를 채울 때 쓴다. 운영자가 고친 다른 칸은 건드리지 않는다.
     * 운영자가 내용을 고친 공고는 원문도 그대로 둔다. 원문이 바뀌면 검수 완료를 지운다.
     */
    @Transactional
    fun changeSourceHtml(jobId: Long, sourceHtml: String) {
        val job = jobReader.readCrawledForUpdate(jobId)
        if (job.isContentEditedByAdmin()) {
            return
        }
        if (saveSource(job, sourceHtml, job.sourceUrl)) {
            clearInspectionOf(job)
        }
    }

    /** 걸러 낸 뒤 글도 이미지도 없으면 저장하지 않는다. 저장된 원문이 있으면 그대로 둔다. 저장한 원문이 전과 다르면 참이다. */
    private fun saveSource(job: Job, sourceHtml: String?, sourceUrl: String?): Boolean {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val sanitized = JobSourceHtmlSanitizer.sanitize(sourceHtml, sourceUrl) ?: return false
        val changed = sanitized != jobSourceReader.readHtml(jobId)
        jobSourceManager.save(jobId, sanitized)
        return changed
    }

    private fun clearInspectionOf(job: Job) {
        if (job.inspectedAt != null) {
            jobManager.clearInspection(job)
        }
    }

    /**
     * 사용자 화면에서 내린다. AI 분석을 끝내 하지 못한 공고처럼 보여 줄 수 없는 공고에 쓴다.
     * 분석은 고용24 공고도 하므로 소유자 없이 수집한 공고 둘 다 숨긴다. 이미 숨긴 공고를 다시 숨겨도 성공한다.
     */
    @Transactional
    fun hide(jobId: Long) {
        jobManager.hide(jobReader.readCollectedForUpdate(jobId))
    }

    /**
     * 모집을 마감한다. 마감일이 없는 상시 채용 공고가 원래 사이트 목록에서 사라졌을 때 AI 서버가 부른다.
     * AI 서버가 등록한 크롤링 공고만 다룬다. 이미 마감한 공고를 다시 마감해도 처음 마감 일시를 지키고 성공한다.
     */
    @Transactional
    fun close(jobId: Long) {
        jobManager.close(jobReader.readCrawledForUpdate(jobId), LocalDateTime.now(clock))
    }

    /** 직무별로 나뉘어 새 공고로 등록된 원래 공고처럼 더는 쓰지 않는 공고를 지운다. 이미 지운 공고를 다시 지워도 성공한다. */
    @Transactional
    fun delete(jobId: Long) {
        jobManager.delete(jobReader.readCrawledForDelete(jobId), LocalDateTime.now(clock))
    }

    /** AI 서버가 등록 응답의 식별자를 잃고 409를 받았을 때 원문 URL로 식별자를 되찾는다. */
    fun getJobId(sourceUrl: String): Long =
        checkNotNull(jobReader.readCrawledBySourceUrl(sourceUrl).id) { "채용공고 식별자가 없습니다." }
}

private fun AiJobCommand.toAppendDto(): JobAppendDto = JobAppendDto(
    companyName = companyName,
    parentCompanyName = parentCompanyName,
    title = title,
    jobField = jobField,
    jobRole = jobRole,
    industry = industry,
    coverImageUrl = coverImageUrl,
    logoUrl = logoUrl,
    employmentType = employmentType,
    experienceType = experienceType,
    experienceMinYears = experienceMinYears,
    educationLevel = educationLevel,
    region = region,
    subRegion = subRegion,
    recruitmentType = recruitmentType,
    recruitmentHeadcount = recruitmentHeadcount,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
    closesWhenFilled = closesWhenFilled,
    autoCloseEnabled = autoCloseEnabled,
    companyAndTeamIntroduction = companyAndTeamIntroduction,
    responsibilities = responsibilities,
    qualifications = qualifications,
    preferredQualifications = preferredQualifications,
    compensation = compensation,
    benefits = benefits,
    hiringProcess = hiringProcess,
    recruitmentNotice = recruitmentNotice,
    applicationMethod = applicationMethod,
    applyEmail = applyEmail,
    inquiryEmail = inquiryEmail,
    sourceUrl = sourceUrl,
    publicationStatus = JobPublicationStatus.PUBLISHED,
)

private fun AiJobCommand.toUpdateDto(): JobUpdateDto = JobUpdateDto(
    companyName = companyName,
    parentCompanyName = parentCompanyName,
    title = title,
    jobField = jobField,
    jobRole = jobRole,
    industry = industry,
    coverImageUrl = coverImageUrl,
    logoUrl = logoUrl,
    employmentType = employmentType,
    experienceType = experienceType,
    experienceMinYears = experienceMinYears,
    educationLevel = educationLevel,
    region = region,
    subRegion = subRegion,
    recruitmentType = recruitmentType,
    recruitmentHeadcount = recruitmentHeadcount,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
    closesWhenFilled = closesWhenFilled,
    autoCloseEnabled = autoCloseEnabled,
    companyAndTeamIntroduction = companyAndTeamIntroduction,
    responsibilities = responsibilities,
    qualifications = qualifications,
    preferredQualifications = preferredQualifications,
    compensation = compensation,
    benefits = benefits,
    hiringProcess = hiringProcess,
    recruitmentNotice = recruitmentNotice,
    applicationMethod = applicationMethod,
    applyEmail = applyEmail,
    inquiryEmail = inquiryEmail,
    sourceUrl = sourceUrl,
)
