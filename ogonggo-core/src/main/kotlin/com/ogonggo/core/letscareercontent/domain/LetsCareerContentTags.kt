package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole

/**
 * 콘텐츠 하나에 붙인 추천용 태그다. 직군·직무는 오공고 채용공고의 값과 같은 enum을 쓴다.
 * 직군·직무가 비어 있으면 어느 직무에나 맞는 콘텐츠라는 뜻이다.
 */
data class LetsCareerContentTags(
    val jobFields: Set<JobField>,
    val jobRoles: Set<JobRole>,
    val topics: Set<LetsCareerContentTopic>,
) {
    val isForAllJobs: Boolean
        get() = jobFields.isEmpty() && jobRoles.isEmpty()
}
