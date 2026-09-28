package com.ogonggo.core.work24.implement.dto

import com.ogonggo.core.work24.implement.Work24CollectionTarget

/**
 * 수집 대상 하나를 받아 온 결과다.
 * 건너뛴 수는 이미 등록된 항목, 실패한 수는 값이 모자라거나 상세 조회가 실패한 항목이다.
 */
data class Work24CollectDto(
    val target: Work24CollectionTarget,
    val pageCount: Int,
    val appendedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
)
