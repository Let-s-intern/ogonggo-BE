package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobListSortKey
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.contentreview.domain.ContentSource
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

@Component
class JobAppender internal constructor(
    private val jobRepository: JobJpaRepository,
    private val clock: Clock,
) {

    fun append(command: JobAppendDto): Job = append(command, LocalDateTime.now(clock))

    /** 목록 정렬 키는 등록 시각의 날짜로 정한다. 같은 날 등록한 공고끼리 섞이도록 무작위 값을 함께 넣는다. */
    fun append(command: JobAppendDto, now: LocalDateTime): Job {
        val source = command.source ?: ContentSource.of(command.ownerUserId)
        return jobRepository.save(
            Job(
                ownerUserId = command.ownerUserId,
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
                publicationStatus = command.publicationStatus,
                source = source,
                externalId = command.externalId,
                listSortKey = JobListSortKey.of(source, now.toLocalDate(), ThreadLocalRandom.current().nextInt()),
                now = now,
            ),
        )
    }
}
