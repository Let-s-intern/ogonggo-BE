package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.review.domain.ContentReviewTargetType
import com.ogonggo.core.review.implement.ContentRejectionManager
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JobManager internal constructor(
    private val jobRepository: JobJpaRepository,
    private val contentRejectionManager: ContentRejectionManager,
) {

    /** 모집 상태는 고친 모집 종료 일시와 기준 시각으로 다시 정한다. 직접 마감한 공고는 마감으로 남는다. */
    fun update(job: Job, command: JobUpdateDto, now: LocalDateTime) {
        job.update(
            companyName = command.companyName,
            parentCompanyName = command.parentCompanyName,
            title = command.title,
            jobField = command.jobField,
            jobRole = command.jobRole,
            industry = command.industry,
            coverImageUrl = command.coverImageUrl,
            logoUrl = command.logoUrl,
            employmentType = command.employmentType,
            experienceType = command.experienceType,
            experienceMinYears = command.experienceMinYears,
            educationLevel = command.educationLevel,
            region = command.region,
            subRegion = command.subRegion,
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
            applyEmail = command.applyEmail,
            inquiryEmail = command.inquiryEmail,
            sourceUrl = command.sourceUrl,
            now = now,
        )
        jobRepository.save(job)
    }

    fun editContent(job: Job, command: JobContentEditDto) = change(job) { editContent(command.title, command.contents) }

    fun publish(job: Job) = change(job) { publish() }

    fun hide(job: Job) = change(job) { hide() }


    fun close(job: Job, now: LocalDateTime) = change(job) { close(now) }

    fun delete(job: Job, now: LocalDateTime) = change(job) { delete(now) }

    /** 모집 종료 일시가 지난 모집 중 공고를 마감하고 마감한 건수를 돌려준다. 직접 마감한 것이 아니므로 마감 처리 일시는 남기지 않는다. */
    @Transactional
    fun closeExpired(now: LocalDateTime): Int = jobRepository.closeExpired(now)

    /** 반려가 풀리면 반려 기록도 함께 지운다. 검수 상태와 기록이 어긋나지 않도록 한 곳에서 처리한다. */
    fun approveReview(job: Job, now: LocalDateTime) {
        change(job) { approveReview() }
        contentRejectionManager.clear(ContentReviewTargetType.JOB, job.requiredId(), now)
    }

    fun rejectReview(job: Job, reason: String, now: LocalDateTime) {
        change(job) { rejectReview() }
        contentRejectionManager.reject(ContentReviewTargetType.JOB, job.requiredId(), reason, now)
    }

    fun requestReview(job: Job, now: LocalDateTime) {
        change(job) { requestReview() }
        contentRejectionManager.clear(ContentReviewTargetType.JOB, job.requiredId(), now)
    }

    private fun change(job: Job, change: Job.() -> Unit) {
        job.change()
        jobRepository.save(job)
    }
}

private fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
