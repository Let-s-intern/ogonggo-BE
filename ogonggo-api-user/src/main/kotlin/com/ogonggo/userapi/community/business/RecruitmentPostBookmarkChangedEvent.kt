package com.ogonggo.userapi.community.business

/** 모집글 북마크가 등록되거나 해제됐다는 사실만 알린다. 수신자가 활성 북마크 수를 다시 센다. */
data class RecruitmentPostBookmarkChangedEvent(val postId: Long)
