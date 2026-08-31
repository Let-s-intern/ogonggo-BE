package com.ogonggo.adminapi.job.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobTagAppender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CrawlerJobService(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobTagAppender: JobTagAppender,
) {

    /**
     * 크롤러가 수집한 공고를 등록한다.
     *
     * 크롤러는 같은 원문을 여러 번 수집할 수 있으므로 이미 등록된 원문은 거절하고
     * 무엇을 다시 수집할지는 크롤러가 판단하게 한다.
     */
    @Transactional
    fun register(command: CrawlerJobRegistrationCommand): Long {
        if (jobReader.existsBySourceUrl(command.sourceUrl)) {
            throw ConflictException(JobErrorCode.JOB_ALREADY_EXISTS)
        }

        val job = jobAppender.append(command.toAppendCommand())
        val jobId = checkNotNull(job.id) { "저장된 채용공고 식별자가 없습니다." }
        jobTagAppender.append(jobId, command.tags)
        return jobId
    }
}

private fun CrawlerJobRegistrationCommand.toAppendCommand(): JobAppendCommand = JobAppendCommand(
    companyName = companyName,
    parentCompanyName = parentCompanyName,
    title = title,
    employmentType = employmentType,
    experienceType = experienceType,
    experienceMinYears = experienceMinYears,
    experienceMaxYears = experienceMaxYears,
    educationLevel = educationLevel,
    region = region,
    recruitmentType = recruitmentType,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
    companyAndTeamIntroduction = companyAndTeamIntroduction,
    responsibilities = responsibilities,
    qualifications = qualifications,
    preferredQualifications = preferredQualifications,
    compensation = compensation,
    benefits = benefits,
    hiringProcess = hiringProcess,
    sourceUrl = sourceUrl,
    publicationStatus = publicationStatus,
)
