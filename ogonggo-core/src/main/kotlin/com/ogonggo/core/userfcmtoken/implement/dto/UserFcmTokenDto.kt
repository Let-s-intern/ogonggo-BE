package com.ogonggo.core.userfcmtoken.implement.dto

import java.util.UUID

data class UserFcmTokenRegisterDto(val token: String, val deviceId: UUID?)

/** 다음 알림 적재 시 기기별 중복 키에 사용할 행 ID와 활성 토큰이다. 로그에 출력하지 않는다. */
class UserFcmTokenDto(val id: Long, val token: String)
