package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface JobManager {
    fun update(job: Job, command: JobUpdateCommand)
    fun publish(job: Job)
    fun hide(job: Job)
    fun archive(job: Job)
    fun close(job: Job, now: LocalDateTime)
    fun delete(job: Job, now: LocalDateTime)
}

@Component
internal class JobManagerImpl(
    private val jobRepository: JobJpaRepository,
) : JobManager {

    override fun update(job: Job, command: JobUpdateCommand) {
        job.update(
            companyName = command.companyName,
            parentCompanyName = command.parentCompanyName,
            title = command.title,
            employmentType = command.employmentType,
            experienceType = command.experienceType,
            experienceMinYears = command.experienceMinYears,
            experienceMaxYears = command.experienceMaxYears,
            educationLevel = command.educationLevel,
            region = command.region,
            recruitmentType = command.recruitmentType,
            recruitmentStartAt = command.recruitmentStartAt,
            recruitmentEndAt = command.recruitmentEndAt,
            companyAndTeamIntroduction = command.companyAndTeamIntroduction,
            responsibilities = command.responsibilities,
            qualifications = command.qualifications,
            preferredQualifications = command.preferredQualifications,
            compensation = command.compensation,
            benefits = command.benefits,
            hiringProcess = command.hiringProcess,
            sourceUrl = command.sourceUrl,
        )
        jobRepository.save(job)
    }

    override fun publish(job: Job) = change(job) { publish() }

    override fun hide(job: Job) = change(job) { hide() }

    override fun archive(job: Job) = change(job) { archive() }

    override fun close(job: Job, now: LocalDateTime) = change(job) { close(now) }

    override fun delete(job: Job, now: LocalDateTime) = change(job) { delete(now) }

    private fun change(job: Job, change: Job.() -> Unit) {
        job.change()
        jobRepository.save(job)
    }
}
