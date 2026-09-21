package com.ogonggo.userapi.job.business

import com.ogonggo.core.bookmark.domain.ApplicationStatus
import com.ogonggo.core.bookmark.domain.BookmarkListCondition
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobRecruitmentStatus
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
        bookmarkCondition: BookmarkListCondition = BookmarkListCondition.NONE,
        recruitmentStatus: JobRecruitmentStatus? = null,
    ): UserJobPageResult {
        val result = jobBookmarkReader.readBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            page = page,
            size = size,
            now = LocalDateTime.now(clock),
            bookmarkCondition = bookmarkCondition,
            recruitmentStatus = recruitmentStatus,
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

    /** 스크랩한 북마크를 지원 준비 중으로 옮긴다. */
    @Transactional
    fun prepare(userId: Long, jobId: Long) {
        jobBookmarkManager.changeApplicationStatus(userId, jobId, ApplicationStatus.PREPARING, LocalDateTime.now(clock))
    }

    /** 지원 준비 중인 북마크를 스크랩으로 되돌린다. */
    @Transactional
    fun cancelPreparation(userId: Long, jobId: Long) {
        jobBookmarkManager.changeApplicationStatus(userId, jobId, ApplicationStatus.SCRAPPED, LocalDateTime.now(clock))
    }
}
