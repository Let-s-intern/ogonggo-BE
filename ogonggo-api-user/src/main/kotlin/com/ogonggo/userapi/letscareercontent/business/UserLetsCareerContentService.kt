package com.ogonggo.userapi.letscareercontent.business

import com.ogonggo.core.job.implement.JobAnalysisReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.implement.LetsCareerContentReader
import com.ogonggo.userapi.letscareercontent.implement.LetsCareerContentRecommendPolicy
import com.ogonggo.userapi.letscareercontent.implement.dto.RecommendedLetsCareerContentDto
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserLetsCareerContentService(
    private val jobReader: JobReader,
    private val jobAnalysisReader: JobAnalysisReader,
    private val letsCareerContentReader: LetsCareerContentReader,
    private val letsCareerContentRecommendPolicy: LetsCareerContentRecommendPolicy,
    private val clock: Clock,
) {

    /** 게시 중인 채용공고에 맞는 렛츠커리어 콘텐츠를 프로그램·자료집·블로그를 섞어 [RECOMMEND_SIZE]개까지 고른다. */
    fun getRecommended(jobId: Long): List<RecommendedLetsCareerContentDto> {
        val job = jobReader.readPublished(jobId)
        val analysis = jobAnalysisReader.readCurrent(job)
        return letsCareerContentRecommendPolicy
            .recommend(job, analysis, letsCareerContentReader.readAll(), LocalDateTime.now(clock), RECOMMEND_SIZE)
            .map(LetsCareerContent::toRecommendedDto)
    }

    companion object {
        const val RECOMMEND_SIZE = 3

        /** 운영 설정은 시크릿으로 통째 덮어써지므로 누락되지 않도록 설정 키로 빼지 않는다. */
        const val LETS_CAREER_WEB_BASE_URL = "https://www.letscareer.co.kr"
    }
}

private fun LetsCareerContent.toRecommendedDto() = RecommendedLetsCareerContentDto(
    kind = kind,
    letsCareerContentId = externalId,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    url = UserLetsCareerContentService.LETS_CAREER_WEB_BASE_URL + path,
    recruitmentStartAt = recruitmentStartAt,
    recruitmentEndAt = recruitmentEndAt,
)
