package com.ogonggo.userapi.letscareercontent.implement.dto

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import java.time.LocalDateTime

/** 공고 상세에 보여 줄 렛츠커리어 콘텐츠 하나다. 누르면 렛츠커리어 웹 상세([url])로 간다. */
data class RecommendedLetsCareerContentDto(
    val kind: LetsCareerContentKind,
    val letsCareerContentId: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val url: String,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
)
