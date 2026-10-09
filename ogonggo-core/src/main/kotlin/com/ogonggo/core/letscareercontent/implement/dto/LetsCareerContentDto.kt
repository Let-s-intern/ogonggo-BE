package com.ogonggo.core.letscareercontent.implement.dto

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentSource

/** 렛츠커리어 목록의 콘텐츠 하나다. 목록 전체로 사본을 덮어쓸 때 Manager에 넣는다. */
data class LetsCareerContentSyncDto(
    val kind: LetsCareerContentKind,
    val externalId: Long,
    val source: LetsCareerContentSource,
)

/** 목록으로 덮어쓴 결과 건수다. */
data class LetsCareerContentSyncResultDto(
    val created: Int,
    val updated: Int,
    val deleted: Int,
)
