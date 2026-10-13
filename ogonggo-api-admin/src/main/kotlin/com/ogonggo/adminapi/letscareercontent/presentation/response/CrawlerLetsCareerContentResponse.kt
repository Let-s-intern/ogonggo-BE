package com.ogonggo.adminapi.letscareercontent.presentation.response

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTopic
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 크롤러 화면에서 보는 렛츠커리어 콘텐츠 하나와 지금 붙은 태그다. */
data class CrawlerLetsCareerContentResponse(
    val contentId: Long,
    val kind: LetsCareerContentKind,
    @field:Schema(description = "렛츠커리어 콘텐츠 식별자")
    val letsCareerContentId: Long,
    val category: String?,
    val title: String,
    val description: String?,
    @field:Schema(description = "렛츠커리어 웹 상세 경로. 도메인은 받는 쪽이 붙입니다")
    val path: String,
    val labels: List<String>,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    @field:Schema(description = "지금 내용에 대한 태그가 있는지. false 면 아직 태그하지 않았거나 태그한 뒤 내용이 바뀌어 추천에 쓰지 않습니다")
    val tagged: Boolean,
    @field:Schema(description = "지금 태그의 직군. tagged 가 false 면 빈 배열")
    val jobFields: List<JobField>,
    @field:Schema(description = "지금 태그의 준비 단계. tagged 가 false 면 빈 배열")
    val topics: List<LetsCareerContentTopic>,
    val taggedAt: LocalDateTime?,
) {
    companion object {
        fun from(content: LetsCareerContent): CrawlerLetsCareerContentResponse {
            val tags = content.currentTags()
            return CrawlerLetsCareerContentResponse(
                contentId = checkNotNull(content.id) { "렛츠커리어 콘텐츠 식별자가 없습니다." },
                kind = content.kind,
                letsCareerContentId = content.externalId,
                category = content.category,
                title = content.title,
                description = content.description,
                path = content.path,
                labels = content.labelList(),
                recruitmentStartAt = content.recruitmentStartAt,
                recruitmentEndAt = content.recruitmentEndAt,
                tagged = tags != null,
                jobFields = tags?.jobFields.orEmpty().sortedBy { it.code },
                topics = tags?.topics.orEmpty().sortedBy { it.code },
                taggedAt = content.taggedAt,
            )
        }
    }
}
