package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.implement.ContentRejectionManager
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class JobManager internal constructor(
    private val jobRepository: JobJpaRepository,
    private val contentRejectionManager: ContentRejectionManager,
) {

    /**
     * 값이 실제로 바뀌었는지 돌려준다.
     * 크롤러는 같은 공고를 여러 번 보내므로, 같은 값이면 검수 상태를 되돌리지 않도록 호출자가 판단하게 한다.
     */
    fun update(job: Job, command: JobUpdateDto): Boolean {
        val changed = job.toUpdateDto() != command
        job.update(
            companyName = command.companyName,
            parentCompanyName = command.parentCompanyName,
            title = command.title,
            jobField = command.jobField,
            jobRole = command.jobRole,
            industry = command.industry,
            coverImageUrl = command.coverImageUrl,
            employmentType = command.employmentType,
            experienceType = command.experienceType,
            experienceMinYears = command.experienceMinYears,
            educationLevel = command.educationLevel,
            region = command.region,
            recruitmentType = command.recruitmentType,
            recruitmentHeadcount = command.recruitmentHeadcount,
            recruitmentStartAt = command.recruitmentStartAt,
            recruitmentEndAt = command.recruitmentEndAt,
            closesWhenFilled = command.closesWhenFilled,
            autoCloseEnabled = command.autoCloseEnabled,
            companyAndTeamIntroduction = command.companyAndTeamIntroduction,
            responsibilities = command.responsibilities,
            qualifications = command.qualifications,
            preferredQualifications = command.preferredQualifications,
            compensation = command.compensation,
            benefits = command.benefits,
            hiringProcess = command.hiringProcess,
            recruitmentNotice = command.recruitmentNotice,
            applicationMethod = command.applicationMethod,
            applicationEmail = command.applicationEmail,
            inquiryEmail = command.inquiryEmail,
            sourceUrl = command.sourceUrl,
        )
        jobRepository.save(job)
        return changed
    }

    fun editContent(job: Job, command: JobContentEditDto) = change(job) { editContent(command.title, command.contents) }

    fun publish(job: Job) = change(job) { publish() }

    fun hide(job: Job) = change(job) { hide() }

    fun archive(job: Job) = change(job) { archive() }

    fun close(job: Job, now: LocalDateTime) = change(job) { close(now) }

    fun delete(job: Job, now: LocalDateTime) = change(job) { delete(now) }

    /** 반려가 풀리면 반려 기록도 함께 지운다. 검수 상태와 기록이 어긋나지 않도록 한 곳에서 처리한다. */
    fun approveReview(job: Job, now: LocalDateTime) {
        change(job) { approveReview() }
        contentRejectionManager.clear(ReviewContentType.JOB, job.requiredId(), now)
    }

    fun rejectReview(job: Job, reason: String, now: LocalDateTime) {
        change(job) { rejectReview() }
        contentRejectionManager.reject(ReviewContentType.JOB, job.requiredId(), reason, now)
    }

    fun requestReview(job: Job, now: LocalDateTime) {
        change(job) { requestReview() }
        contentRejectionManager.clear(ReviewContentType.JOB, job.requiredId(), now)
    }

    private fun change(job: Job, change: Job.() -> Unit) {
        job.change()
        jobRepository.save(job)
    }
}

private fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }

private fun Job.toUpdateDto(): JobUpdateDto = JobUpdateDto(
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
