package com.ogonggo.userapi.adposting.business

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.job.business.UserJobSummary

/** 공개 광고 공고 한 건이다. 대상 종류에 맞는 요약 하나만 있다. 광고주·기간·켜짐·순서는 싣지 않는다. */
data class UserAdPostingResult(
    val id: Long,
    val placement: AdPostingPlacement,
    val targetType: AdPostingTargetType,
    val job: UserJobSummary? = null,
    val bootcamp: UserBootcampSummary? = null,
)
