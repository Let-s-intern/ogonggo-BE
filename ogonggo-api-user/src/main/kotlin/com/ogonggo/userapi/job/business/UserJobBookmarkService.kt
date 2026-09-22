package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmarkSearchCondition
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.implement.JobBookmarkManager
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserJobBookmarkService(
    private val jobReader: JobReader,
    private val jobBookmarkReader: JobBookmarkReader,
    private val jobBookmarkManager: JobBookmarkManager,
    private val jobMetricReader: JobMetricReader,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    fun getBookmarks(
        userId: Long,
        condition: JobSearchCondition,
        page: Int,
        size: Int,
        bookmarkCondition: JobBookmarkSearchCondition = JobBookmarkSearchCondition.NONE,
    ): UserJobPageResult {
        val result = jobBookmarkReader.readBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            page = page,
            size = size,
            now = LocalDateTime.now(clock),
            bookmarkCondition = bookmarkCondition,
        )
        val jobIds = result.jobs.map(Job::requiredId)
        return UserJobPageResult.from(
            result = result,
            bookmarkedJobIds = jobIds.toSet(),
            metrics = jobMetricReader.readAll(jobIds),
        )
    }

    @Transactional
    fun addBookmark(userId: Long, jobId: Long) {
        jobReader.readPublished(jobId)
        jobBookmarkManager.append(userId, jobId, LocalDateTime.now(clock))
        eventPublisher.publishEvent(JobBookmarkChangedEvent(jobId))
    }

    @Transactional
    fun deleteBookmark(userId: Long, jobId: Long) {
        jobReader.readIncludingDeleted(jobId)
        jobBookmarkManager.delete(userId, jobId, LocalDateTime.now(clock))
        eventPublisher.publishEvent(JobBookmarkChangedEvent(jobId))
    }

    /** 단계 사이에 선후 관계가 없어 어느 단계로든 옮긴다. */
    @Transactional
    fun changeApplicationStatus(userId: Long, jobId: Long, status: JobApplicationStatus) {
        jobBookmarkManager.changeApplicationStatus(userId, jobId, status, LocalDateTime.now(clock))
    }
}
