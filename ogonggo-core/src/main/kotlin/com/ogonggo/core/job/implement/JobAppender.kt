package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.springframework.stereotype.Component

interface JobAppender {
    fun append(command: JobAppendCommand): Job
}

@Component
internal class JobAppenderImpl(
    private val jobRepository: JobJpaRepository,
) : JobAppender {

    override fun append(command: JobAppendCommand): Job =
        jobRepository.save(
            Job(
                ownerUserId = command.ownerUserId,
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
                publicationStatus = command.publicationStatus,
            ),
        )
}
