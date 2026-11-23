package com.ogonggo.core.work24jobfield.domain

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.jpa.BaseTimeEntity
import com.querydsl.core.annotations.PropertyType
import com.querydsl.core.annotations.QueryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 고용24 채용정보를 매일 받을 때 이 직군의 공고를 받을지 정하는 설정이다. 직군마다 한 행이고 직군이 기본 키다.
 * 운영자가 배포 없이 관리자 API로 켜고 끈다. 행이 없는 직군은 [DEFAULT_ENABLED_FIELDS]로 정한다.
 * 지우는 경로가 없어 삭제 일시를 두지 않는다.
 */
@Entity
@Table(name = "work24_job_fields")
class Work24JobField internal constructor(
    jobField: JobField,
    enabled: Boolean,
) : BaseTimeEntity() {

    // QueryDSL APT가 JobField를 숫자 경로로 만들어 컴파일이 깨지므로 enum 경로로 고정한다.
    @field:QueryType(PropertyType.ENUM)
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "job_field", length = 100)
    var jobField: JobField = jobField /* 직군 */
        protected set

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = enabled /* 수집 켜짐 여부 */
        protected set

    fun replace(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        /**
         * 행이 없는 직군의 값이다. 배포만으로 수집 대상이 바뀌지 않게 이 설정이 생기기 전에 받던 다섯 직군이다
         * (2026-10-06 LC-3444 마케팅·인사·기획·영업·개발, 2026-10-12 직군을 다섯 묶음으로 합침).
         */
        val DEFAULT_ENABLED_FIELDS: Set<JobField> = setOf(
            JobField.MARKETING_ADVERTISING,
            JobField.HR_GENERAL_AFFAIRS,
            JobField.PLANNING_STRATEGY,
            JobField.SALES,
            JobField.IT_DEVELOPMENT,
        )
    }
}
