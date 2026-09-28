package com.ogonggo.core.work24.implement

import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.domain.Work24Item
import com.ogonggo.core.work24.implement.dto.Work24ItemAppendDto
import com.ogonggo.core.work24.persistence.Work24ItemJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class Work24ItemAppender internal constructor(
    private val work24ItemRepository: Work24ItemJpaRepository,
) {

    /**
     * 처음 보는 항목만 저장하고 저장한 수를 돌려준다. 이미 있는 항목은 내용이 달라도 건너뛴다.
     *
     * 유니크 제약 위반을 삼키므로 호출자가 트랜잭션을 열어 둔 채로 부르면 안 된다.
     * 제약 위반은 그 트랜잭션을 롤백 대상으로 만들고, 예외를 잡아도 커밋 시점에 다시 터진다.
     */
    fun appendNew(api: Work24Api, items: List<Work24ItemAppendDto>): Int {
        val candidates = items.distinctBy { it.externalId }
        if (candidates.isEmpty()) {
            return 0
        }

        val existing = work24ItemRepository.findExternalIds(api, candidates.map { it.externalId }).toSet()
        return candidates
            .filterNot { it.externalId in existing }
            .count { item ->
                try {
                    work24ItemRepository.saveAndFlush(
                        Work24Item(api = api, externalId = item.externalId, payload = item.payload),
                    )
                    true
                } catch (exception: DataIntegrityViolationException) {
                    // 수집은 스케줄러 잠금으로 한 곳에서만 돌지만, 겹치더라도 먼저 저장한 쪽을 남긴다.
                    false
                }
            }
    }
}
