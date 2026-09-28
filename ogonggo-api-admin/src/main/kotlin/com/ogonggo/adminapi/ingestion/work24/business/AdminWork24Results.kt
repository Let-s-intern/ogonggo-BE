package com.ogonggo.adminapi.ingestion.work24.business

import com.ogonggo.adminapi.ingestion.work24.implement.Work24CollectionTarget
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CollectDto

/** 수집 대상 하나의 결과다. */
sealed interface AdminWork24CollectResult {
    val target: Work24CollectionTarget

    data class Collected(val result: Work24CollectDto) : AdminWork24CollectResult {
        override val target: Work24CollectionTarget get() = result.target
    }

    data class Skipped(override val target: Work24CollectionTarget) : AdminWork24CollectResult

    data class Failed(override val target: Work24CollectionTarget) : AdminWork24CollectResult
}
