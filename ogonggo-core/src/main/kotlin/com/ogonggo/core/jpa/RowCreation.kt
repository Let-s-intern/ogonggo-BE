package com.ogonggo.core.jpa

import org.springframework.dao.DataIntegrityViolationException

/**
 * 유니크 키로 하나만 있어야 하는 행을, 없으면 만들고 다른 요청이 먼저 만들었으면 그 행을 쓴다.
 * 지표 행처럼 첫 갱신 시점에 만들어져 동시 요청이 겹칠 수 있는 행에 쓴다.
 *
 * - [create]는 호출자와 분리된 트랜잭션(`REQUIRES_NEW`)에서 저장해야 한다. 유니크 제약 위반이 호출자의 트랜잭션까지
 *   롤백 대상으로 만들면 상대가 만든 행을 읽지도, 이어서 갱신하지도 못한다.
 * - 갱신보다 먼저 부른다. 없는 행을 UPDATE로 먼저 찾으면 MySQL이 그 자리에 gap lock을 걸어,
 *   같은 스레드가 여는 생성 트랜잭션의 INSERT가 잠금 대기 시간 초과로 실패한다.
 */
inline fun ensureRowCreated(exists: () -> Boolean, create: () -> Unit) {
    if (exists()) return
    try {
        create()
    } catch (_: DataIntegrityViolationException) {
        // 다른 요청이 같은 행을 먼저 만들었다. 생성 트랜잭션만 롤백되었으므로 그 행을 그대로 쓴다.
    }
}
