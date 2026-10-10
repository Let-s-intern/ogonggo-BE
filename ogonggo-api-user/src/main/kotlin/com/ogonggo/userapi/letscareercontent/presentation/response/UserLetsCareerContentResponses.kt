package com.ogonggo.userapi.letscareercontent.presentation.response

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.userapi.letscareercontent.implement.dto.RecommendedLetsCareerContentDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserRecommendedLetsCareerContentResponse(
    @field:Schema(description = "콘텐츠 종류. 챌린지·라이브·VOD·가이드북·서류 진단은 프로그램, MATERIAL은 무료 자료집, BLOG는 블로그")
    val kind: LetsCareerContentKind,
    @field:Schema(description = "렛츠커리어의 콘텐츠 식별자. 종류마다 따로 매긴다")
    val letsCareerContentId: Long,
    val title: String,
    @field:Schema(description = "짧은 설명. 없으면 null")
    val description: String?,
    val thumbnailUrl: String?,
    @field:Schema(description = "렛츠커리어 웹 상세 주소. 카드를 누르면 이 주소로 이동한다")
    val url: String,
    @field:Schema(description = "모집 시작 일시. 모집이 없는 콘텐츠(VOD·자료집·블로그 등)는 null")
    val recruitmentStartAt: LocalDateTime?,
    @field:Schema(description = "모집 종료 일시. 모집이 없는 콘텐츠는 null")
    val recruitmentEndAt: LocalDateTime?,
) {
    companion object {
        fun from(content: RecommendedLetsCareerContentDto) = UserRecommendedLetsCareerContentResponse(
            kind = content.kind,
            letsCareerContentId = content.letsCareerContentId,
            title = content.title,
            description = content.description,
            thumbnailUrl = content.thumbnailUrl,
            url = content.url,
            recruitmentStartAt = content.recruitmentStartAt,
            recruitmentEndAt = content.recruitmentEndAt,
        )
    }
}
