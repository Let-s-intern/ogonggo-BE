package com.ogonggo.core.adposting.implement.dto

import com.ogonggo.core.adposting.domain.AdPostingPeriod
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.job.domain.Job
import java.time.LocalDateTime

/** 자리 맨 뒤에 붙일 광고 공고다. 대상이 있는지와 같은 자리 중복은 부르는 쪽이 먼저 본다. */
data class AdPostingAppendDto(
    val placement: AdPostingPlacement,
    val targetId: Long,
    val advertiserId: Long,
    val contractId: Long?,
    val period: AdPostingPeriod,
    val enabled: Boolean,
)

/** 저장된 값과 합친 수정 값이다. 자리와 대상은 바꾸지 않는다. */
data class AdPostingUpdateDto(
    val advertiserId: Long,
    val contractId: Long?,
    val period: AdPostingPeriod,
    val enabled: Boolean,
)

/**
 * 광고 공고 하나의 대상이다. 대상이 지워졌으면 제목·회사가 빈 문자열이고 엔티티가 없다.
 * [available]은 대상이 지금 공개이고 모집 중인가다. 아니면 켜 두어도 사용자에게 나가지 않는다.
 */
data class AdPostingTargetDto(
    val title: String,
    val companyName: String,
    val available: Boolean,
    val job: Job? = null,
    val bootcamp: Bootcamp? = null,
) {
    companion object {
        val MISSING = AdPostingTargetDto(title = "", companyName = "", available = false)
    }
}

/** 광고주 하나의 광고 공고 수다. 노출 중은 켜져 있고 기간 안이며 대상이 공개·모집 중인 것이다. */
data class AdPostingCountDto(
    val adPostingCount: Int,
    val activeAdPostingCount: Int,
    /** 노출 끝 가운데 가장 늦은 것. 끝이 정해진 광고 공고가 없으면 null이다. */
    val latestEndAt: LocalDateTime?,
) {
    companion object {
        val NONE = AdPostingCountDto(0, 0, null)
    }
}
