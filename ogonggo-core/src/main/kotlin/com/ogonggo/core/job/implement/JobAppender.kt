package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobListSortKey
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.job.implement.event.JobRecruitmentDeadlineChangedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

@Component
class JobAppender internal constructor(
    private val jobRepository: JobJpaRepository,
    private val jobQueryRepository: JobQueryRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    @Transactional
    fun append(command: JobAppendDto): Job = append(command, LocalDateTime.now(clock))

    /**
     * 목록 정렬 키는 등록 시각의 날짜로 정한다. 같은 날 등록한 공고끼리 섞이도록 무작위 값을 함께 넣는다.
     * 지금 오늘의 공고(PICK)와 직군이 같으면 같은 날 공고 중 앞에 오도록 PICK 직군 비트를 켠다.
     */
    @Transactional
    fun append(command: JobAppendDto, now: LocalDateTime): Job {
        val source = command.source ?: ContentSource.of(command.ownerUserId)
        val savedJob = jobRepository.save(
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
                listSortKey = JobListSortKey.of(
                    source = source,
                    registeredOn = now.toLocalDate(),
                    pickField = command.jobField?.let { it in jobQueryRepository.findPublishedTodayJobFields() } ?: false,
                    shuffle = ThreadLocalRandom.current().nextInt(),
                ),
                now = now,
            ),
        )
        savedJob.recruitmentEndAt?.let { recruitmentEndAt ->
            eventPublisher.publishEvent(
                JobRecruitmentDeadlineChangedEvent(
                    jobId = checkNotNull(savedJob.id),
                    previousEndAt = null,
                    recruitmentEndAt = recruitmentEndAt,
                ),
            )
        }
        return savedJob
    }
}
