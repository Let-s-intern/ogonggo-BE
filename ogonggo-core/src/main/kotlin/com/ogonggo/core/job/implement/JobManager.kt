package com.ogonggo.core.job.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractManager
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.dto.JobContentEditDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.implement.event.JobRecruitmentDeadlineChangedEvent
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JobManager internal constructor(
    private val jobRepository: JobJpaRepository,
    private val contentRejectionManager: ContentRejectionManager,
    private val advertisementContractManager: AdvertisementContractManager,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /** 모집 상태는 고친 모집 종료 일시와 기준 시각으로 다시 정한다. 직접 마감한 공고는 마감으로 남는다. */
    @Transactional
    fun update(job: Job, command: JobUpdateDto, now: LocalDateTime) {
        val previousEndAt = job.recruitmentEndAt
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
        if (previousEndAt != job.recruitmentEndAt) {
            eventPublisher.publishEvent(
                JobRecruitmentDeadlineChangedEvent(
                    jobId = job.requiredId(),
                    previousEndAt = previousEndAt,
                    recruitmentEndAt = job.recruitmentEndAt,
                ),
            )
        }
    }

    fun editContent(job: Job, command: JobContentEditDto) = change(job) { editContent(command.title, command.contents) }

    fun markContentEdited(job: Job, userId: Long, now: LocalDateTime) = change(job) { markContentEdited(userId, now) }

    fun publish(job: Job) = change(job) { publish() }

    fun hide(job: Job) = change(job) { hide() }

    fun changePriorityExposed(job: Job, priorityExposed: Boolean) = change(job) { changePriorityExposed(priorityExposed) }


    fun close(job: Job, now: LocalDateTime) = change(job) { close(now) }

    fun delete(job: Job, now: LocalDateTime) = change(job) { delete(now) }

    /**
     * 이미 검수 완료한 공고는 처음 확인한 시각과 관리자를 지킨다. 공고 수정 일시는 바꾸지 않는다.
     * 공고를 잠가 읽은 뒤 부르며, 이 뒤로 [job] 객체는 영속성 컨텍스트에서 떨어진다.
     */
    fun markInspected(job: Job, userId: Long, now: LocalDateTime) {
        require(userId > 0) { "관리자 식별자는 양수여야 합니다." }
        jobRepository.markInspected(job.requiredId(), userId, now)
    }

    /** 검수 완료가 없어도 성공한다. 공고 수정 일시는 바꾸지 않는다. */
    fun clearInspection(job: Job) {
        jobRepository.clearInspection(job.requiredId())
    }

    /** 모집 종료 일시가 지난 모집 중 공고를 마감하고 마감한 건수를 돌려준다. 직접 마감한 것이 아니므로 마감 처리 일시는 남기지 않는다. */
    fun closeExpired(now: LocalDateTime): Int = jobRepository.closeExpired(now)

    /**
     * 반려가 풀리면 반려 기록도 함께 지운다. 검수 상태와 기록이 어긋나지 않도록 한 곳에서 처리한다.
     * 비즈니스 공고 계약의 상태도 같은 이유로 여기서 검수 상태에 맞춘다.
     */
    fun approveReview(job: Job, now: LocalDateTime) {
        change(job) { approveReview(now) }
        contentRejectionManager.clear(ContentReviewTargetType.JOB, job.requiredId(), now)
        followReview(job)
    }

    fun rejectReview(job: Job, reason: String, now: LocalDateTime) {
        change(job) { rejectReview() }
        contentRejectionManager.reject(ContentReviewTargetType.JOB, job.requiredId(), reason, now)
        followReview(job)
    }

    fun requestReview(job: Job, now: LocalDateTime) {
        change(job) { requestReview() }
        contentRejectionManager.clear(ContentReviewTargetType.JOB, job.requiredId(), now)
        followReview(job)
    }

    private fun followReview(job: Job) {
        val reviewStatus = checkNotNull(job.reviewStatus) { "검수한 채용공고의 검수 상태가 없습니다." }
        advertisementContractManager.followReview(AdvertisementContractPromotionTarget.JOB, job.requiredId(), reviewStatus)
    }

    private fun change(job: Job, change: Job.() -> Unit) {
        job.change()
        jobRepository.save(job)
    }
}

private fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
