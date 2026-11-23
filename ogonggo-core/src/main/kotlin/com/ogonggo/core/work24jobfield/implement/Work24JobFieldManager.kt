package com.ogonggo.core.work24jobfield.implement

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.work24jobfield.domain.Work24JobField
import com.ogonggo.core.work24jobfield.persistence.Work24JobFieldJpaRepository
import org.springframework.stereotype.Component

@Component
class Work24JobFieldManager internal constructor(
    private val work24JobFieldRepository: Work24JobFieldJpaRepository,
) {

    /** 직군의 수집 켜짐 여부를 바꾼다. 행이 없으면 만든다. 행이 없을 때 두 요청이 함께 만들어도 기본 키가 두 번째 행을 막는다. */
    fun replace(jobField: JobField, enabled: Boolean) {
        val saved = work24JobFieldRepository.findById(jobField).orElse(null)
        if (saved == null) {
            work24JobFieldRepository.save(Work24JobField(jobField, enabled))
            return
        }
        saved.replace(enabled)
    }
}
