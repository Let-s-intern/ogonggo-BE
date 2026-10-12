package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField

/**
 * 렛츠커리어 콘텐츠 추천에서 쓰는 직군 묶음이다 (2026-10-12 결정).
 *
 * 렛츠커리어 콘텐츠는 다섯 직군(IT·개발, 마케팅·광고, 영업, HR·총무, 기획·전략)으로만 태그한다.
 * 다른 직군 공고는 가까운 묶음으로 옮겨 비교한다. 묶음이 없는 직군(게임, 법률 등)의 공고는
 * 어느 직무에나 맞는 콘텐츠만 추천받는다.
 */
object LetsCareerContentFieldGroup {

    /** 콘텐츠에 붙일 수 있는 직군이다. */
    val TAGGABLE: Set<JobField> = linkedSetOf(
        JobField.IT_DEVELOPMENT,
        JobField.MARKETING_ADVERTISING,
        JobField.SALES,
        JobField.HR_GENERAL_AFFAIRS,
        JobField.PLANNING_STRATEGY,
    )

    private val MERGED: Map<JobField, JobField> = mapOf(
        JobField.AI_DATA to JobField.IT_DEVELOPMENT,
        JobField.MERCHANDISING to JobField.PLANNING_STRATEGY,
        JobField.DESIGN to JobField.PLANNING_STRATEGY,
        // 경영관리 묶음. 렛츠커리어 직무 분류도 HR 을 '경영관리'로 부른다
        JobField.ACCOUNTING_TAX_FINANCE to JobField.HR_GENERAL_AFFAIRS,
        JobField.CUSTOMER_SERVICE_TM to JobField.HR_GENERAL_AFFAIRS,
        JobField.MEDIA_ENTERTAINMENT to JobField.MARKETING_ADVERTISING,
    )

    /** 직군이 속한 묶음이다. 묶음이 없으면 `null`이다. */
    fun of(field: JobField): JobField? = if (field in TAGGABLE) field else MERGED[field]
}
