package com.ogonggo.core.userfcmtoken.implement.dto

import java.util.UUID
import com.ogonggo.core.user.domain.UserStatus

data class UserFcmTokenRegisterDto(val token: String, val deviceId: UUID?)

/** 다음 알림 적재 시 기기별 중복 키에 사용할 행 ID와 활성 토큰이다. 로그에 출력하지 않는다. */
class UserFcmTokenDto(val id: Long, val token: String)

class UserFcmTokenRecipientDto(val userId: Long, val status: UserStatus, val tokens: List<UserFcmTokenDto>)
