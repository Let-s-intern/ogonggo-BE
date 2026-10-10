package com.ogonggo.adminapi.concern.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility

/** 관리자가 고른 고민글들의 노출을 한꺼번에 바꾼다. 하나라도 바꿀 수 없으면 아무것도 바꾸지 않는다. */
data class AdminConcernVisibilityChangeCommand(
    val concernIds: List<Long>,
    val visibility: AdminContentVisibility,
)
