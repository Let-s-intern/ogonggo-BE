package com.ogonggo.core.bootcamp.domain

/**
 * 부트캠프 목록에서 클라이언트가 고를 수 있는 선택 필터다.
 * 공개 여부나 삭제 여부처럼 서버가 강제하는 조건은 담지 않는다.
 * 필터가 늘어나도 조회 계약의 시그니처가 바뀌지 않도록 여기에만 값을 추가한다.
 */
data class BootcampSearchCondition(
    val tuitionType: TuitionType? = null,
    /** 모집 상태. 공개 목록이 다루는 RECRUITING과 CLOSED만 의미가 있다. */
    val status: BootcampStatus? = null,
    /** 운영 회사명 또는 프로그램명에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
) {
    companion object {
        val NONE = BootcampSearchCondition()
    }
}
