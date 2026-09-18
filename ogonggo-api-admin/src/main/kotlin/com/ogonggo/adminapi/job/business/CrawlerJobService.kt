package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobTagAppender
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.review.domain.ReviewStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class CrawlerJobService(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobTagAppender: JobTagAppender,
    private val clock: Clock,
) {

    /**
     * 크롤러가 수집한 공고를 초안·검수 대기로 등록한다.
     *
     * 크롤러는 원문에 없는 값도 AI로 유추해 채우므로 운영자가 검수에서 승인해야 노출된다.
     * 같은 원문을 다시 등록하면 거절하고, 크롤러는 등록 응답의 식별자로 공고를 교체한다.
     */
    @Transactional
    fun register(command: CrawlerJobRegistrationCommand): Long {
        if (jobReader.existsBySourceUrl(command.job.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        val job = jobAppender.append(command.job.toAppendDto())
        val jobId = checkNotNull(job.id) { "저장된 채용공고 식별자가 없습니다." }
        jobTagAppender.append(jobId, command.tags)
        return jobId
    }

    /**
     * 다시 수집·분류한 값으로 공고 전체를 바꾼다. 운영자가 관리자 콘솔에서 고친 내용도 크롤러 값으로 덮는다.
     *
     * 값이 실제로 바뀌면 승인·반려된 공고를 다시 검수 대기로 돌리고 노출을 끈다. 사람이 확인한 값과 달라졌기 때문이다.
     * 같은 값을 다시 보내면 상태를 건드리지 않는다. 크롤러가 주기마다 보내도 승인된 공고가 내려가지 않게 하기 위해서다.
     */
    @Transactional
    fun replace(jobId: Long, command: CrawlerJobCommand) {
        val job = jobReader.readCrawledForUpdate(jobId)
        if (command.sourceUrl != job.sourceUrl && jobReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        val changed = jobManager.update(job, command.toUpdateDto())
        if (changed && job.reviewStatus in REVIEWED) {
            jobManager.requestReview(job, LocalDateTime.now(clock))
        }
    }

    /** 직무별로 나뉘어 새 공고로 등록된 원래 공고처럼 더는 쓰지 않는 공고를 지운다. 이미 지운 공고를 다시 지워도 성공한다. */
    @Transactional
    fun delete(jobId: Long) {
        jobManager.delete(jobReader.readCrawledForDelete(jobId), LocalDateTime.now(clock))
    }

    /** 크롤러가 등록 응답의 식별자를 잃고 409를 받았을 때 원문 URL로 식별자를 되찾는다. */
    fun getJobId(sourceUrl: String): Long =
        checkNotNull(jobReader.readCrawledBySourceUrl(sourceUrl).id) { "채용공고 식별자가 없습니다." }

    companion object {
        private val REVIEWED = setOf(ReviewStatus.APPROVED, ReviewStatus.REJECTED)
    }
}

private fun CrawlerJobCommand.toAppendDto(): JobAppendDto = JobAppendDto(
    companyName = companyName,
    parentCompanyName = parentCompanyName,
    title = title,
    jobField = jobField,
    jobRole = jobRole,
    industry = industry,
    coverImageUrl = coverImageUrl,
    employmentType = employmentType,
    experienceType = experienceType,
    experienceMinYears = experienceMinYears,
    educationLevel = educationLevel,
    region = region,
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
    applicationEmail = applicationEmail,
    inquiryEmail = inquiryEmail,
    sourceUrl = sourceUrl,
    publicationStatus = JobPublicationStatus.DRAFT,
    requiresReview = true,
)

private fun CrawlerJobCommand.toUpdateDto(): JobUpdateDto = JobUpdateDto(
    companyName = companyName,
    parentCompanyName = parentCompanyName,
    title = title,
    jobField = jobField,
    jobRole = jobRole,
    industry = industry,
    coverImageUrl = coverImageUrl,
    employmentType = employmentType,
    experienceType = experienceType,
    experienceMinYears = experienceMinYears,
    educationLevel = educationLevel,
    region = region,
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
    applicationEmail = applicationEmail,
    inquiryEmail = inquiryEmail,
    sourceUrl = sourceUrl,
)
