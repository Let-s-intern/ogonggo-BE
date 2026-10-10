package com.ogonggo.userapi.user.implement

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode

/**
 * 정지·탈퇴 회원은 글쓰기, 북마크, 지원 같은 동작을 할 수 없다.
 * 로그인 규칙은 탈퇴 회원의 재로그인 허용 여부가 정해지지 않아 따로 바뀔 수 있으므로 `SignInValidator`가 둔다.
 */
internal fun UserStatus.requireActive() {
    when (this) {
        UserStatus.ACTIVE -> Unit
        UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
        UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
    }
}
