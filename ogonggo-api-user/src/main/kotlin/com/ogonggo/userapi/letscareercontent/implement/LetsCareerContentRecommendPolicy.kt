package com.ogonggo.userapi.letscareercontent.implement

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTopic
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 채용공고 하나에 맞는 렛츠커리어 콘텐츠를 고른다.
 *
 * 1. 태그가 없거나 모집 기간이 아닌 콘텐츠는 빼고, 다른 직군 전용 콘텐츠도 뺀다.
 * 2. 직무가 같으면 +5, 직군이 같으면 +3, 공고가 요구하는 준비 단계(제출 서류·전형 등)와 겹치면 하나에 +2(최대 두 개).
 * 3. 점수가 같으면 공고 식별자로 정한 순서로 고른다. 같은 공고는 언제 열어도 같은 추천이 나오고,
 *    같은 직무의 다른 공고끼리는 조금씩 다른 콘텐츠가 나온다.
 * 4. 한 갈래(프로그램·자료집·블로그)가 [MAX_PER_GROUP]개를 넘지 않게 섞는다. 그래도 모자라면 남은 것으로 채운다.
 */
@Component
class LetsCareerContentRecommendPolicy {

    fun recommend(
        job: Job,
        analysis: JobAnalysisContent?,
        contents: List<LetsCareerContent>,
        now: LocalDateTime,
        size: Int,
    ): List<LetsCareerContent> {
        val jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." }
        val jobTopics = topicsOf(job, analysis)
        val ranked = contents
            .mapNotNull { content -> score(job, jobTopics, content, now)?.let { content to it } }
            .sortedWith(
                compareByDescending<Pair<LetsCareerContent, Int>> { it.second }
                    .thenBy { tieBreaker(jobId, it.first) },
            )
            .map { it.first }

        val picked = mutableListOf<LetsCareerContent>()
        ranked.forEach { content ->
            if (picked.size < size && picked.count { groupOf(it) == groupOf(content) } < MAX_PER_GROUP) {
                picked += content
            }
        }
        ranked.forEach { content ->
            if (picked.size < size && content !in picked) {
                picked += content
            }
        }
        return picked
    }

    /** 추천하지 않을 콘텐츠면 `null`이다. */
    private fun score(job: Job, jobTopics: Set<LetsCareerContentTopic>, content: LetsCareerContent, now: LocalDateTime): Int? {
        val tags = content.currentTags() ?: return null
        if (!isRecruiting(content, now)) {
            return null
        }
        val roleMatched = job.jobRole != null && job.jobRole in tags.jobRoles
        val fieldMatched = job.jobField != null && job.jobField in fieldsOf(tags)
        if (!tags.isForAllJobs && !roleMatched && !fieldMatched) {
            return null
        }
        val topicScore = minOf(tags.topics.count { it in jobTopics }, MAX_TOPIC_MATCHES) * TOPIC_SCORE
        return (if (roleMatched) ROLE_SCORE else 0) + (if (fieldMatched) FIELD_SCORE else 0) + topicScore
    }

    /** 직무만 붙은 콘텐츠도 그 직무의 직군 공고에는 맞는다. */
    private fun fieldsOf(tags: LetsCareerContentTags) = tags.jobFields + tags.jobRoles.map { it.jobField }

    private fun isRecruiting(content: LetsCareerContent, now: LocalDateTime): Boolean {
        val startAt = content.recruitmentStartAt
        val endAt = content.recruitmentEndAt
        return (startAt == null || !startAt.isAfter(now)) && (endAt == null || !endAt.isBefore(now))
    }

    private fun tieBreaker(jobId: Long, content: LetsCareerContent): Int =
        mix(jobId * 31 + content.kind.code * 1_000_003L + content.externalId)

    /** 공고 식별자가 하나 달라도 순서가 크게 바뀌도록 섞는다(splitmix64). */
    private fun mix(value: Long): Int {
        var z = value + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return (z xor (z ushr 31)).toInt()
    }

    private fun groupOf(content: LetsCareerContent): LetsCareerContentKind = when (content.kind) {
        LetsCareerContentKind.MATERIAL -> LetsCareerContentKind.MATERIAL
        LetsCareerContentKind.BLOG -> LetsCareerContentKind.BLOG
        else -> LetsCareerContentKind.CHALLENGE
    }

    /**
     * 공고가 요구하는 준비 단계다. 공고 분석의 제출 서류·자소서·전형 칸과 공고 본문의 전형·안내 칸을 함께 본다.
     * 분석이 아직 없는 공고도 본문만으로 고를 수 있게 하기 위해서다.
     */
    internal fun topicsOf(job: Job, analysis: JobAnalysisContent?): Set<LetsCareerContentTopic> {
        val submission = analysis?.submission
        val text = listOfNotNull(
            submission?.documents?.value,
            submission?.documents?.note,
            submission?.essay?.value,
            submission?.essay?.note,
            submission?.process?.value,
            submission?.process?.note,
            job.hiringProcess,
            job.recruitmentNotice,
        ).joinToString("\n")

        val topics = TOPIC_KEYWORDS
            .filterValues { keywords -> keywords.any { text.contains(it, ignoreCase = true) } }
            .keys
            .toMutableSet()
        if (submission?.essay?.value != null) {
            topics += LetsCareerContentTopic.PERSONAL_STATEMENT
        }
        if (!analysis?.competencies.isNullOrEmpty()) {
            topics += LetsCareerContentTopic.EXPERIENCE_SUMMARY
        }
        if (job.employmentType == JobEmploymentType.INTERN || job.employmentType == JobEmploymentType.WORK_EXPERIENCE) {
            topics += LetsCareerContentTopic.INTERNSHIP
        }
        if (job.experienceType != JobExperienceType.EXPERIENCED) {
            topics += LetsCareerContentTopic.CAREER_START
        }
        return topics
    }

    companion object {
        /** 같은 갈래(프로그램·자료집·블로그)에서 고르는 최대 개수다. 모자라면 넘어서 채운다. */
        const val MAX_PER_GROUP = 2

        private const val ROLE_SCORE = 5
        private const val FIELD_SCORE = 3
        private const val TOPIC_SCORE = 2
        private const val MAX_TOPIC_MATCHES = 2

        private val TOPIC_KEYWORDS: Map<LetsCareerContentTopic, List<String>> = mapOf(
            LetsCareerContentTopic.RESUME to listOf("이력서"),
            LetsCareerContentTopic.PERSONAL_STATEMENT to listOf("자기소개서", "자소서"),
            LetsCareerContentTopic.PORTFOLIO to listOf("포트폴리오"),
            LetsCareerContentTopic.INTERVIEW to listOf("면접", "인터뷰"),
            LetsCareerContentTopic.WRITTEN_TEST to listOf("인적성", "필기", "코딩테스트", "코딩 테스트", "NCS", "직무적성", "과제"),
        )
    }
}
