package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.Job
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

    fun getBookmarks(userId: Long, page: Int, size: Int): UserJobPageResult {
        val result = jobBookmarkReader.readBookmarkedPublishedPage(userId, page, size)
        val jobIds = result.jobs.map(Job::requiredId)
        return UserJobPageResult.from(
            result = result,
            bookmarkedJobIds = jobIds.toSet(),
            metrics = jobMetricReader.readAll(jobIds),
        )
    }

    @Transactional
    fun addBookmark(userId: Long, jobId: Long) {
        jobReader.readPublishedForUpdate(jobId)
        jobBookmarkManager.append(userId, jobId)
        eventPublisher.publishEvent(JobBookmarkChangedEvent(jobId))
    }

    @Transactional
    fun deleteBookmark(userId: Long, jobId: Long) {
        jobReader.readIncludingDeletedForUpdate(jobId)
        jobBookmarkManager.delete(userId, jobId, LocalDateTime.now(clock))
        eventPublisher.publishEvent(JobBookmarkChangedEvent(jobId))
    }
}
