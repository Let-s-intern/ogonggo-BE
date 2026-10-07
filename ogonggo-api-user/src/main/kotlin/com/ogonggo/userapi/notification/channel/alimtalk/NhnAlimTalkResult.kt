package com.ogonggo.userapi.notification.channel.alimtalk

/** NHN 응답을 접수·중복 키·실패·해석 불가 결과로 정규화한다. Accepted는 provider 접수만 의미한다. */
sealed interface NhnAlimTalkResult {
    data class Accepted(val requestId: String?) : NhnAlimTalkResult
    /** 같은 멱등성 키가 10분 내 이미 사용됐다는 NHN 응답. 최초 요청의 실제 접수 여부와는 구분한다. */
    data object DuplicateIdempotencyKey : NhnAlimTalkResult
    data class Rejected(val resultCode: String?) : NhnAlimTalkResult
    data object InvalidResponse : NhnAlimTalkResult
}
