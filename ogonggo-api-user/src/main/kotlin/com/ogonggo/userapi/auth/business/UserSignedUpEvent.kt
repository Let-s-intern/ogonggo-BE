package com.ogonggo.userapi.auth.business

import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import java.time.LocalDateTime

/**
 * 가입 완료 시점의 불변 스냅샷이다. AFTER_COMMIT 비동기 listener가 나중에 처리하므로
 * listener에서 사용자 Repository를 다시 조회하지 않고, 이벤트 발행 시점의 값을 사용한다.
 */
data class UserSignedUpEvent(
    val userId: Long,
    val name: String?,
    val email: String?,
    val phoneNum: String?,
    val authProvider: LetsCareerAuthProvider?,
    val joinedAt: LocalDateTime,
)
