package com.ogonggo.core.job.domain

import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion

/**
 * 채용공고 목록에서 클라이언트가 고를 수 있는 선택 필터다.
 * 게시 상태나 삭제 여부처럼 서버가 강제하는 조건은 담지 않는다.
 * 필터가 늘어나도 조회 계약의 시그니처가 바뀌지 않도록 여기에만 값을 추가한다.
 */
data class JobSearchCondition(
    val employmentType: EmploymentType? = null,
    val experienceType: ExperienceType? = null,
    /** 직군. 직무까지 정한 공고도 직군이 같으면 걸린다. */
    val jobField: JobField? = null,
    /** 직무. 비어 있으면 거르지 않고, 여러 개면 그중 하나라도 맞는 공고가 걸린다. */
    val jobRoles: Set<JobRole> = emptySet(),
    /** 근무 시·도. 시·군·구까지 정한 공고도 시·도가 같으면 걸린다. */
    val region: Region? = null,
    /** 근무 시·군·구. */
    val subRegion: SubRegion? = null,
    /** 회사명 또는 공고 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
) {
    companion object {
        val NONE = JobSearchCondition()
    }
}
