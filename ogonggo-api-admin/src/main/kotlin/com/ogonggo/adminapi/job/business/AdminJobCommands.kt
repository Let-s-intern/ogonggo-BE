package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.review.domain.ContentReviewStatus

/**
 * 관리자가 채용공고의 운영 값과 내용을 고친다. 넘어온 값만 바꾼다.
 * 모든 값을 늘 함께 보내게 하면 화면이 건드리지 않은 값까지 되돌려 쓰고, 그 사이 다른 곳에서 바뀐 값이 조용히 덮인다.
 */
data class AdminJobUpdateCommand(
    val visibility: AdminContentVisibility? = null,
    /** 승인이나 검수 대기로만 바꾼다. 반려는 사유가 필요해 검수 화면에서 처리한다. */
    val reviewStatus: ContentReviewStatus? = null,
    val title: String? = null,
    /** 값이 null인 칸은 비운다. */
    val contents: Map<JobContentField, String?> = emptyMap(),
)

/** 관리자가 고른 채용공고들의 노출을 한꺼번에 바꾼다. 하나라도 바꿀 수 없으면 아무것도 바꾸지 않는다. */
data class AdminJobVisibilityChangeCommand(
    val jobIds: List<Long>,
    val visibility: AdminContentVisibility,
)
