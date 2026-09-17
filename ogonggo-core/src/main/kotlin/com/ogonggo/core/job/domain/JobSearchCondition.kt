package com.ogonggo.core.job.domain

/**
 * 채용공고 목록에서 클라이언트가 고를 수 있는 선택 필터다.
 * 게시 상태나 삭제 여부처럼 서버가 강제하는 조건은 담지 않는다.
 * 필터가 늘어나도 조회 계약의 시그니처가 바뀌지 않도록 여기에만 값을 추가한다.
 */
data class JobSearchCondition(
    val employmentType: EmploymentType? = null,
    val experienceType: ExperienceType? = null,
    /** 직군. 공고의 직군과 정확히 같은지로 거른다. 비어 있으면 거르지 않는다. */
    val jobField: String? = null,
    /** 직무. 공고의 직무와 정확히 같은지로 거른다. 비어 있으면 거르지 않는다. */
    val jobRole: String? = null,
    /** 회사명 또는 공고 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
) {
    companion object {
        val NONE = JobSearchCondition()
    }
}
