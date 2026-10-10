package com.ogonggo.core.paging

const val MAX_PAGE_SIZE = 100

/**
 * 목록 조회의 페이지 범위를 확인한다. 페이지 번호는 0부터 시작한다.
 * 각 API가 요청 단계에서 먼저 검증하므로 여기서 실패하면 호출하는 코드의 버그다.
 */
fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..MAX_PAGE_SIZE) { "페이지 크기는 1 이상 $MAX_PAGE_SIZE 이하여야 합니다." }
}
