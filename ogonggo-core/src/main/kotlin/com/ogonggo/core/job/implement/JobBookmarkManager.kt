package com.ogonggo.core.job.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface JobBookmarkManager {
    fun append(userId: Long, jobId: Long)
    fun delete(userId: Long, jobId: Long, now: LocalDateTime)
}

@Component
internal class JobBookmarkManagerImpl(
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) : JobBookmarkManager {

    override fun append(userId: Long, jobId: Long) {
        val bookmark = jobBookmarkRepository.findByJobIdAndUserId(jobId, userId)

        if (bookmark?.isActive == true) {
            throw ConflictException(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS)
        }

        try {
            if (bookmark == null) {
                jobBookmarkRepository.saveAndFlush(JobBookmark(jobId = jobId, userId = userId))
            } else {
                bookmark.restore()
                jobBookmarkRepository.saveAndFlush(bookmark)
            }
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS)
        }
    }

    override fun delete(userId: Long, jobId: Long, now: LocalDateTime) {
        val bookmark = jobBookmarkRepository.findByJobIdAndUserId(jobId, userId)
            ?.takeIf(JobBookmark::isActive)
            ?: return

        bookmark.delete(now)
        jobBookmarkRepository.saveAndFlush(bookmark)
    }
}
