package com.ogonggo.core.work24.implement.dto

import com.ogonggo.core.work24.implement.Work24CollectionTarget

/** 목록에서 꺼낸 항목 하나다. */
data class Work24ItemAppendDto(
    val externalId: String,
    val payload: String,
)

/** 수집 대상 하나를 받아 온 결과다. */
data class Work24CollectDto(
    val target: Work24CollectionTarget,
    val pageCount: Int,
    val fetchedCount: Int,
    val appendedCount: Int,
)
