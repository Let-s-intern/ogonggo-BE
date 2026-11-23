package com.ogonggo.core.work24jobfield.implement

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.work24jobfield.domain.Work24JobField
import com.ogonggo.core.work24jobfield.persistence.Work24JobFieldJpaRepository
import org.springframework.stereotype.Component

@Component
class Work24JobFieldReader internal constructor(
    private val work24JobFieldRepository: Work24JobFieldJpaRepository,
) {

    /** 모든 직군의 수집 켜짐 여부다. [JobField] 순서를 따른다. 행이 없는 직군은 기본값을 쓴다. */
    fun findAll(): Map<JobField, Boolean> {
        val saved = work24JobFieldRepository.findAll().associate { it.jobField to it.enabled }
        return JobField.entries.associateWith { field ->
            saved[field] ?: (field in Work24JobField.DEFAULT_ENABLED_FIELDS)
        }
    }

    /** 고용24 채용정보를 받을 직군이다. */
    fun findEnabled(): Set<JobField> = findAll().filterValues { it }.keys
}
