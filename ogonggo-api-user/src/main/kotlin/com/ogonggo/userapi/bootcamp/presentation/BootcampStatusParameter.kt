package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.userapi.error.InvalidRequestParameterException

private val SELECTABLE_STATUSES = listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED)

/**
 * 공개 목록과 북마크 목록은 모집중과 모집 마감만 다루므로 임시저장은 고를 수 없다.
 * 그대로 넘기면 항상 빈 목록이 나가 클라이언트가 잘못 보냈다는 사실을 알 수 없다.
 */
internal fun validatePublicStatus(status: BootcampStatus?) {
    if (status != null && status !in SELECTABLE_STATUSES) {
        throw InvalidRequestParameterException(
            "status",
            "고를 수 있는 모집 상태는 ${SELECTABLE_STATUSES.joinToString()}입니다.",
        )
    }
}
