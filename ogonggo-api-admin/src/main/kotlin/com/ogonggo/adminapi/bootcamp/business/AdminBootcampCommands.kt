package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.contentreview.domain.ContentReviewStatus

/**
 * 관리자가 부트캠프의 운영 값과 내용을 고친다. 채용공고와 같이 넘어온 값만 바꾼다.
 * 커리큘럼과 파트너사는 구조가 있는 값이라 여기서 고치지 않는다.
 */
data class AdminBootcampUpdateCommand(
    val visibility: AdminContentVisibility? = null,
    /** 승인이나 검수 대기로만 바꾼다. 반려는 사유가 필요해 검수 화면에서 처리한다. */
    val reviewStatus: ContentReviewStatus? = null,
    val title: String? = null,
    /** 값이 null인 칸은 비운다. 상세 내용은 비울 수 없다. */
    val contents: Map<BootcampContentField, String?> = emptyMap(),
)

/** 관리자가 고른 부트캠프들의 노출을 한꺼번에 바꾼다. 하나라도 바꿀 수 없으면 아무것도 바꾸지 않는다. */
data class AdminBootcampVisibilityChangeCommand(
    val bootcampIds: List<Long>,
    val visibility: AdminContentVisibility,
)
