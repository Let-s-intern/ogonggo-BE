package com.ogonggo.adminapi.content.business

import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.job.domain.JobSortType

/**
 * 관리자 콘솔 목록의 정렬 기준이다. 채용공고와 부트캠프가 같은 값을 쓴다.
 * 등록일순은 식별자 역순과 같으므로 사용자 목록의 최신순으로 옮긴다.
 * 조회수순은 조회 수가 같으면 등록일 역순으로 순서를 확정한다.
 */
enum class AdminContentSortType(
    override val code: Int,
    override val desc: String,
    val jobSortType: JobSortType,
    val bootcampSortType: BootcampSortType,
) : EnumField {
    REGISTERED_AT(1, "등록일순", JobSortType.LATEST, BootcampSortType.LATEST),
    VIEW_COUNT(2, "조회수순", JobSortType.VIEW_COUNT, BootcampSortType.VIEW_COUNT),
}
