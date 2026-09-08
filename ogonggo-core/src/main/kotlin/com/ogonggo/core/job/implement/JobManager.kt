package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class JobManager internal constructor(
    private val jobRepository: JobJpaRepository,
) {

    fun update(job: Job, command: JobUpdateDto) {
        job.update(
            companyName = command.companyName,
            parentCompanyName = command.parentCompanyName,
            companyLogoUrl = command.companyLogoUrl,
            title = command.title,
            jobField = command.jobField,
            coverImageUrl = command.coverImageUrl,
            employmentType = command.employmentType,
            experienceType = command.experienceType,
            experienceMinYears = command.experienceMinYears,
            experienceMaxYears = command.experienceMaxYears,
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
            sourceUrl = command.sourceUrl,
        )
        jobRepository.save(job)
    }

    fun publish(job: Job) = change(job) { publish() }

    fun hide(job: Job) = change(job) { hide() }

    fun archive(job: Job) = change(job) { archive() }

    fun close(job: Job, now: LocalDateTime) = change(job) { close(now) }

    fun delete(job: Job, now: LocalDateTime) = change(job) { delete(now) }

    private fun change(job: Job, change: Job.() -> Unit) {
        job.change()
        jobRepository.save(job)
    }
}
