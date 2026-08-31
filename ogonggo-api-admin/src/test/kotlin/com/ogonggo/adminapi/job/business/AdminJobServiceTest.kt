package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobUpdateCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminJobServiceTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val jobAppender = Mockito.mock(JobAppender::class.java)
    private val jobManager = Mockito.mock(JobManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminJobService(jobReader, jobAppender, jobManager, clock)

    @Test
    fun `공고를 생성하고 식별자를 반환한다`() {
        val command = TestJobCommands.append()
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.id).thenReturn(1L)
        Mockito.`when`(jobAppender.append(command)).thenReturn(job)

        assertEquals(1L, service.create(command))
        Mockito.verify(jobAppender).append(command)
    }

    @Test
    fun `공고를 잠금 조회한 뒤 수정한다`() {
        val command = TestJobCommands.update()
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readForUpdate(1L)).thenReturn(job)

        service.update(1L, command)

        Mockito.verify(jobReader).readForUpdate(1L)
        Mockito.verify(jobManager).update(job, command)
    }

    @Test
    fun `공고를 잠금 조회한 뒤 게시한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readForUpdate(1L)).thenReturn(job)

        service.publish(1L)

        Mockito.verify(jobReader).readForUpdate(1L)
        Mockito.verify(jobManager).publish(job)
    }

    @Test
    fun `공고 삭제 시 전달한 시간을 Manager에 넘긴다`() {
        val job = Mockito.mock(Job::class.java)
        val now = LocalDateTime.of(2026, 8, 26, 12, 0)
        Mockito.`when`(jobReader.readForUpdate(1L)).thenReturn(job)

        service.delete(1L, now)

        Mockito.verify(jobManager).delete(job, now)
    }

    @Test
    fun `현재 시각을 생략하면 서울 기준 Clock을 사용한다`() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(jobReader.readForUpdate(1L)).thenReturn(job)

        service.close(1L)

        Mockito.verify(jobManager).close(job, LocalDateTime.of(2026, 8, 27, 12, 0))
    }
}
