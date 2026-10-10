package com.ogonggo.adminapi.letscareercontent.presentation.response

import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import io.swagger.v3.oas.annotations.media.Schema

/** 크롤러가 태그할 렛츠커리어 콘텐츠 하나다. */
data class CrawlerLetsCareerContentTagTargetResponse(
    @field:Schema(description = "오공고의 렛츠커리어 콘텐츠 사본 식별자. 태그를 보낼 때 경로에 씁니다")
    val contentId: Long,
    val kind: LetsCareerContentKind,
    @field:Schema(description = "렛츠커리어의 분류 enum 이름(챌린지 유형, 서류 진단 유형, 자료집 유형, 블로그 카테고리)")
    val category: String?,
    val title: String,
    val description: String?,
    @field:Schema(description = "직무·추천 대상·해시태그 같은 분류 단서")
    val labels: List<String>,
    @field:Schema(description = "지금 내용의 해시. 태그를 보낼 때 그대로 돌려줍니다")
    val contentHash: String,
) {
    companion object {
        fun from(content: LetsCareerContent) = CrawlerLetsCareerContentTagTargetResponse(
            contentId = checkNotNull(content.id) { "렛츠커리어 콘텐츠 식별자가 없습니다." },
            kind = content.kind,
            category = content.category,
            title = content.title,
            description = content.description,
            labels = content.labelList(),
            contentHash = content.contentHash,
        )
    }
}
