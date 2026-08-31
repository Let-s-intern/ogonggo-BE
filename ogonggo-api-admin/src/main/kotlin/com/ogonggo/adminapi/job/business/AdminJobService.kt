package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobUpdateCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminJobService(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val clock: Clock,
) {

    @Transactional
    fun create(command: JobAppendCommand): Long {
        val job = jobAppender.append(command)
        return checkNotNull(job.id) { "저장된 채용공고 식별자가 없습니다." }
    }

    @Transactional
    fun update(jobId: Long, command: JobUpdateCommand) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.update(job, command)
    }

    @Transactional
    fun publish(jobId: Long) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.publish(job)
    }

    @Transactional
    fun hide(jobId: Long) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.hide(job)
    }

    @Transactional
    fun archive(jobId: Long) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.archive(job)
    }

    @Transactional
    fun close(jobId: Long, now: LocalDateTime = LocalDateTime.now(clock)) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.close(job, now)
    }

    @Transactional
    fun delete(jobId: Long, now: LocalDateTime = LocalDateTime.now(clock)) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.delete(job, now)
    }
}
