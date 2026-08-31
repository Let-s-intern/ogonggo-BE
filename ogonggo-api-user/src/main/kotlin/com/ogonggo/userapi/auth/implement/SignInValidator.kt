package com.ogonggo.userapi.auth.implement

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import org.springframework.stereotype.Component

/**
 * 렛츠커리어 교환 로그인과 기업 로그인이 같은 상태 규칙을 쓴다.
 *
 * 탈퇴 사용자의 재로그인 허용 여부는 아직 정해지지 않았다(확인 필요).
 * 현재 도메인은 탈퇴를 되돌릴 수 없다고 선언하고 있으므로 로그인을 막는다.
 */
@Component
class SignInValidator {

    fun validate(status: UserStatus) {
        when (status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}
