package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField

/**
 * 렛츠커리어 콘텐츠에 붙일 수 있는 직군이다 (2026-10-12 결정).
 *
 * 콘텐츠는 다섯 직군(IT·개발, 마케팅·광고, 영업, HR·총무, 기획·전략)으로만 태그한다. 같은 날 [JobField]도
 * 여섯 직군을 이 다섯으로 합쳤으므로 AI·데이터 같은 직무의 공고도 묶음 직군으로 저장돼 있다.
 * 나머지 직군(게임, 법률 등)의 공고는 어느 직무에나 맞는 콘텐츠만 추천받는다.
 */
object LetsCareerContentFieldGroup {

    val TAGGABLE: Set<JobField> = linkedSetOf(
        JobField.IT_DEVELOPMENT,
        JobField.MARKETING_ADVERTISING,
        JobField.SALES,
        JobField.HR_GENERAL_AFFAIRS,
        JobField.PLANNING_STRATEGY,
    )

    /** 콘텐츠 추천에서 쓰는 직군이다. 다섯 직군이 아니면 `null`이다. */
    fun of(field: JobField): JobField? = field.takeIf { it in TAGGABLE }
}
