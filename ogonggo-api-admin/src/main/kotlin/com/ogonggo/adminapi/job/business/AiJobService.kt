package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.job.implement.JobSourceHtmlSanitizer
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceManager
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
        saveSource(jobId, command.job.sourceHtml, command.job.sourceUrl)
        return jobId
    }

    /**
     * 다시 수집·분류한 값으로 공고 전체를 바꾼다. 운영자가 관리자 콘솔에서 고친 내용도 AI 서버 값으로 덮는다.
     * 게시 상태는 바꾸지 않는다.
     */
    @Transactional
    fun replace(jobId: Long, command: AiJobCommand) {
        val job = jobReader.readCrawledForUpdate(jobId)
        if (command.sourceUrl != job.sourceUrl && jobReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        jobManager.update(job, command.toUpdateDto(), LocalDateTime.now(clock))
        saveSource(jobId, command.sourceHtml, command.sourceUrl)
    }

    /** 원문만 바꾼다. 원문 저장 전에 보낸 공고를 채울 때 쓴다. 운영자가 고친 다른 칸은 건드리지 않는다. */
    @Transactional
    fun changeSourceHtml(jobId: Long, sourceHtml: String) {
        val job = jobReader.readCrawledForUpdate(jobId)
        saveSource(jobId, sourceHtml, job.sourceUrl)
    }

    /** 걸러 낸 뒤 글도 이미지도 없으면 저장하지 않는다. 저장된 원문이 있으면 그대로 둔다. */
    private fun saveSource(jobId: Long, sourceHtml: String?, sourceUrl: String?) {
        JobSourceHtmlSanitizer.sanitize(sourceHtml, sourceUrl)?.let { jobSourceManager.save(jobId, it) }
    }

    /**
     * 사용자 화면에서 내린다. AI 분석을 끝내 하지 못한 공고처럼 보여 줄 수 없는 공고에 쓴다.
     * 분석은 고용24 공고도 하므로 소유자 없이 수집한 공고 둘 다 숨긴다. 이미 숨긴 공고를 다시 숨겨도 성공한다.
     */
    @Transactional
    fun hide(jobId: Long) {
        jobManager.hide(jobReader.readCollectedForUpdate(jobId))
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
