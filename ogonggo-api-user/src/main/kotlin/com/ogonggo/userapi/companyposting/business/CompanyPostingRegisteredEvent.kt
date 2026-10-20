package com.ogonggo.userapi.companyposting.business

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import java.time.LocalDateTime

/**
 * 기업회원이 채용공고나 부트캠프를 새로 등록했다는 사실이다.
 *
 * 커밋 뒤 다른 스레드에서 처리하므로 발행 시점의 값을 담아 둔다.
 * 수신자가 공고를 다시 조회하지 않아도 알림을 만들 수 있어야 한다.
 */
data class CompanyPostingRegisteredEvent(
    val type: CompanyPostingType,
    val postingId: Long,
    val companyName: String,
    val title: String,
    val accountEmail: String?,
    /** 부트캠프만 쓴다 */
    val recruitmentEndAt: LocalDateTime? = null,
    /** 채용공고만 쓴다 */
    val jobField: JobField? = null,
    /** 채용공고만 쓴다 */
    val jobRole: JobRole? = null,
)

enum class CompanyPostingType(val desc: String) {
    JOB("채용공고"),
    BOOTCAMP("부트캠프"),
}
