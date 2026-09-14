package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 채용공고의 모집 상태다. 저장하지 않고 마감 처리 일시와 모집 종료 일시로 계산한다.
 * 마감 처리됐거나 종료 일시가 지났으면 마감이고, 그 밖에는 모집 중이다.
 * 상시 채용은 종료 일시가 없으므로 마감 처리 전까지 모집 중이다.
 */
enum class JobRecruitmentStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    RECRUITING(1, "모집 중"),
    CLOSED(2, "모집 마감"),
}
