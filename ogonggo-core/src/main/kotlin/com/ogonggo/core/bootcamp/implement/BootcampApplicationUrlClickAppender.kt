package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampApplicationUrlClick
import com.ogonggo.core.bootcamp.persistence.BootcampApplicationUrlClickJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class BootcampApplicationUrlClickAppender internal constructor(
    private val bootcampApplicationUrlClickRepository: BootcampApplicationUrlClickJpaRepository,
) {

    /**

    * 같은 사용자가 같은 부트캠프를 여러 번 눌러도 최초 기록만 남기고 조용히 넘어간다.

    *

    * 유니크 제약 위반을 삼키므로 호출자가 트랜잭션을 열어 둔 채로 부르면 안 된다.

    * 제약 위반은 그 트랜잭션을 롤백 대상으로 만들고, 예외를 잡아도 커밋 시점에 다시 터진다.

    */

    fun append(userId: Long, bootcampId: Long) {
        if (bootcampApplicationUrlClickRepository.existsByBootcampIdAndUserId(bootcampId, userId)) {
            return
        }

        try {
            bootcampApplicationUrlClickRepository.saveAndFlush(
                BootcampApplicationUrlClick(bootcampId = bootcampId, userId = userId),
            )
        } catch (exception: DataIntegrityViolationException) {
            // 같은 사용자가 버튼을 연속으로 눌러 동시에 저장되면 유니크 제약이 막는다.
            // 저장이 자기 트랜잭션에서 롤백되고 기록은 상대 요청이 이미 남겼으므로 성공으로 본다.
        }
    }
}
