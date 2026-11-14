package com.ogonggo.adminapi.job.presentation.response

import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.job.domain.Job

/** AI 서버가 원문을 열어 볼 상시 채용 공고 하나다. 제목과 회사명은 원문 화면에 공고가 아직 보이는지 가릴 때 쓴다. */
data class AiJobSourceCheckTargetResponse(
    val jobId: Long,
    val source: ContentSource,
    val sourceUrl: String,
    val companyName: String,
    val title: String,
) {
    companion object {
        fun from(job: Job): AiJobSourceCheckTargetResponse = AiJobSourceCheckTargetResponse(
            jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." },
            source = job.source,
            sourceUrl = checkNotNull(job.sourceUrl) { "원문 URL이 없는 공고는 확인 대상이 아닙니다." },
            companyName = job.companyName,
            title = job.title,
        )
    }
}
