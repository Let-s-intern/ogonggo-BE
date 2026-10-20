package com.ogonggo.userapi.companyposting.business

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
    val recruitmentEndAt: LocalDateTime?,
)

enum class CompanyPostingType(val desc: String) {
    JOB("채용공고"),
    BOOTCAMP("부트캠프"),
}
