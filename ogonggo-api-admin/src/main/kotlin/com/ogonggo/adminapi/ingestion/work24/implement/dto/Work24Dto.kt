package com.ogonggo.adminapi.ingestion.work24.implement.dto

import com.ogonggo.adminapi.ingestion.work24.implement.Work24CollectionTarget

/**
 * 수집 대상 하나를 받아 온 결과다.
 * 건너뛴 수는 이미 등록된 항목, 제외한 수는 수집 조건에 맞지 않아 상세를 부르지 않은 항목(시급·일급, 분류할 수 없는 직종의 공고),
 * 실패한 수는 값이 모자라거나 상세 조회가 실패한 항목이다.
 */
data class Work24CollectDto(
    val target: Work24CollectionTarget,
    val pageCount: Int,
    val appendedCount: Int,
    val skippedCount: Int,
    val excludedCount: Int,
    val failedCount: Int,
)
