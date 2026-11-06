package com.ogonggo.userapi.adposting.business

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.adposting.implement.dto.AdPostingTargetDto
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.userapi.adposting.implement.UserAdPostingCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class UserAdPostingServiceTest {

    private val adPostingReader = Mockito.mock(AdPostingReader::class.java)
    private val adPostingTargetReader = Mockito.mock(AdPostingTargetReader::class.java)
    private val jobMetricReader = Mockito.mock(JobMetricReader::class.java)
    private val clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL)
    private val service = UserAdPostingService(
        adPostingReader = adPostingReader,
        adPostingTargetReader = adPostingTargetReader,
        jobMetricReader = jobMetricReader,
        bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java),
        userAdPostingCache = UserAdPostingCache(clock),
        clock = clock,
    )

    @Test
    fun `노출 중이고 대상이 나갈 수 있는 광고만 순서대로 싣고 북마크는 늘 false다`() {
        // given
        val shown = adPosting(id = 1, live = true)
        val off = adPosting(id = 2, live = false)
        val closedTarget = adPosting(id = 3, live = true)
        val job = job(13)
        Mockito.`when`(adPostingReader.readAllByPlacement(AdPostingPlacement.JOB_LIST)).thenReturn(listOf(shown, off, closedTarget))
        val targets = mapOf(
            1L to AdPostingTargetDto("백엔드 개발자", "오공고", available = true, job = job),
            3L to AdPostingTargetDto("마감 공고", "오공고", available = false),
        )
        Mockito.`when`(adPostingTargetReader.read(listOf(shown, closedTarget), NOW)).thenReturn(targets)
        Mockito.`when`(jobMetricReader.readAll(listOf(13L))).thenReturn(mapOf(13L to JobMetricDto(viewCount = 7, bookmarkCount = 2, commentCount = 0)))

        // when
        val result = service.getAdPostings(AdPostingPlacement.JOB_LIST)

        // then
        assertEquals(listOf(1L), result.map { it.id })
        assertEquals(AdPostingTargetType.JOB, result.single().targetType)
        assertEquals(13L, result.single().job?.id)
        assertEquals(7L, result.single().job?.viewCount)
        assertFalse(checkNotNull(result.single().job).bookmarked)
    }

    private fun adPosting(id: Long, live: Boolean): AdPosting = Mockito.mock(AdPosting::class.java).also { adPosting ->
        Mockito.`when`(adPosting.id).thenReturn(id)
        Mockito.`when`(adPosting.isLive(NOW)).thenReturn(live)
    }

    private fun job(id: Long): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.id).thenReturn(id)
        Mockito.`when`(job.publicRegisteredAt()).thenReturn(NOW)
        Mockito.`when`(job.companyName).thenReturn("오공고")
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.employmentType).thenReturn(JobEmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(JobExperienceType.EXPERIENCED)
        Mockito.`when`(job.educationLevel).thenReturn(JobEducationLevel.ANY)
        Mockito.`when`(job.recruitmentType).thenReturn(JobRecruitmentType.ALWAYS_OPEN)
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
