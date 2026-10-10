package com.ogonggo.userapi.letscareercontent.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTopic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class LetsCareerContentRecommendPolicyTest {

    private val policy = LetsCareerContentRecommendPolicy()

    @Test
    fun `다른 직군 전용 콘텐츠는 빼고 같은 직무 콘텐츠를 먼저 고른다`() {
        // given
        val job = job(jobRole = MARKETING_ROLE)
        val sameRole = content(1, tags(roles = setOf(MARKETING_ROLE)))
        val sameField = content(2, tags(fields = setOf(JobField.MARKETING_ADVERTISING)))
        val forAll = content(3, tags())
        val otherField = content(4, tags(fields = setOf(JobField.IT_DEVELOPMENT)))

        // when
        val result = policy.recommend(job, null, listOf(otherField, forAll, sameField, sameRole), NOW, 3)

        // then
        assertEquals(listOf(1L, 2L, 3L), result.map { it.externalId })
    }

    @Test
    fun `공고가 자기소개서를 받으면 자기소개서 콘텐츠를 다른 공통 콘텐츠보다 먼저 고른다`() {
        // given
        val job = job(jobRole = MARKETING_ROLE, hiringProcess = "서류전형(자기소개서) → 1차 면접")
        val essay = content(1, tags(topics = setOf(LetsCareerContentTopic.PERSONAL_STATEMENT)))
        val portfolio = content(2, tags(topics = setOf(LetsCareerContentTopic.PORTFOLIO)))

        // when
        val result = policy.recommend(job, null, listOf(portfolio, essay), NOW, 1)

        // then
        assertEquals(listOf(1L), result.map { it.externalId })
    }

    @Test
    fun `태그가 없거나 모집 기간이 아닌 콘텐츠는 고르지 않는다`() {
        val job = job(jobRole = MARKETING_ROLE)
        val untagged = content(1, null)
        val closed = content(2, tags(), recruitmentEndAt = NOW.minusDays(1))
        val upcoming = content(3, tags(), recruitmentStartAt = NOW.plusDays(1))

        assertEquals(emptyList<LetsCareerContent>(), policy.recommend(job, null, listOf(untagged, closed, upcoming), NOW, 3))
    }

    @Test
    fun `한 갈래에서 두 개까지만 고르고 남은 자리는 다른 갈래로 채운다`() {
        // given
        val job = job(jobRole = MARKETING_ROLE)
        val blogs = (1L..3L).map { content(it, tags(roles = setOf(MARKETING_ROLE)), kind = LetsCareerContentKind.BLOG) }
        val material = content(10, tags(), kind = LetsCareerContentKind.MATERIAL)

        // when
        val result = policy.recommend(job, null, blogs + material, NOW, 3)

        // then
        assertEquals(2, result.count { it.kind == LetsCareerContentKind.BLOG })
        assertEquals(LetsCareerContentKind.MATERIAL, result.last().kind)
    }

    @Test
    fun `점수가 같으면 공고마다 다른 순서로 고르고 같은 공고는 늘 같은 결과다`() {
        // given
        val contents = (1L..20L).map { content(it, tags()) }

        // when
        val first = policy.recommend(job(id = 1L, jobRole = MARKETING_ROLE), null, contents, NOW, 3).map { it.externalId }
        val again = policy.recommend(job(id = 1L, jobRole = MARKETING_ROLE), null, contents.reversed(), NOW, 3).map { it.externalId }
        val other = policy.recommend(job(id = 2L, jobRole = MARKETING_ROLE), null, contents, NOW, 3).map { it.externalId }

        // then
        assertEquals(first, again)
        assertNotEquals(first, other)
    }

    private fun job(
        id: Long = 1L,
        jobRole: JobRole? = null,
        hiringProcess: String? = null,
    ): Job = Mockito.mock(Job::class.java).also { job ->
        Mockito.`when`(job.id).thenReturn(id)
        Mockito.`when`(job.jobRole).thenReturn(jobRole)
        Mockito.`when`(job.jobField).thenReturn(jobRole?.jobField)
        Mockito.`when`(job.hiringProcess).thenReturn(hiringProcess)
        Mockito.`when`(job.employmentType).thenReturn(JobEmploymentType.FULL_TIME)
        Mockito.`when`(job.experienceType).thenReturn(JobExperienceType.EXPERIENCED)
    }

    private fun content(
        externalId: Long,
        tags: LetsCareerContentTags?,
        kind: LetsCareerContentKind = LetsCareerContentKind.CHALLENGE,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ): LetsCareerContent = Mockito.mock(LetsCareerContent::class.java).also { content ->
        Mockito.`when`(content.kind).thenReturn(kind)
        Mockito.`when`(content.externalId).thenReturn(externalId)
        Mockito.`when`(content.currentTags()).thenReturn(tags)
        Mockito.`when`(content.recruitmentStartAt).thenReturn(recruitmentStartAt)
        Mockito.`when`(content.recruitmentEndAt).thenReturn(recruitmentEndAt)
    }

    private fun tags(
        fields: Set<JobField> = emptySet(),
        roles: Set<JobRole> = emptySet(),
        topics: Set<LetsCareerContentTopic> = emptySet(),
    ) = LetsCareerContentTags(jobFields = fields, jobRoles = roles, topics = topics)

    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 9, 12, 0)
        private val MARKETING_ROLE = JobRole.entries.first { it.jobField == JobField.MARKETING_ADVERTISING }
    }
}
