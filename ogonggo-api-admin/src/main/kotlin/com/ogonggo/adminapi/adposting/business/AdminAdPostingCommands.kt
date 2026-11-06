package com.ogonggo.adminapi.adposting.business

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import java.time.LocalDateTime
import java.util.Optional

/** 등록 본문이다. 검증 전 값을 그대로 담는다. 프런트 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다. */
data class AdminAdPostingCreateCommand(
    val placement: AdPostingPlacement?,
    val targetId: Long?,
    val advertiserId: Long?,
    val contractId: Long?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean?,
)

/**
 * 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`contractId`, `startAt`, `endAt`)은
 * 보내지 않으면 null, null을 보내면 빈 [Optional]이다.
 */
data class AdminAdPostingUpdateCommand(
    val advertiserId: Long? = null,
    val contractId: Optional<Long>? = null,
    val startAt: Optional<LocalDateTime>? = null,
    val endAt: Optional<LocalDateTime>? = null,
    val enabled: Boolean? = null,
)

/** 자리 안 순서 바꾸기 본문이다. 그 자리의 광고 공고 식별자를 빠짐없이 새 순서대로 받는다. */
data class AdminAdPostingOrderCommand(
    val placement: AdPostingPlacement?,
    val adPostingIds: List<Long?>?,
)
