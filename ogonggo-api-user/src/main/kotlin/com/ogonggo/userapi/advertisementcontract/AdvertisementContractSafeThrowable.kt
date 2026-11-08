package com.ogonggo.userapi.advertisementcontract

/** 외부 provider 예외의 메시지와 cause를 버리고 원래 예외의 stack frame만 보존한다. */
internal fun Throwable.toSafeLogThrowable(): Throwable = Throwable("외부 provider 예외 상세 정보는 로그에서 제외했습니다.").apply {
    stackTrace = this@toSafeLogThrowable.stackTrace
}
