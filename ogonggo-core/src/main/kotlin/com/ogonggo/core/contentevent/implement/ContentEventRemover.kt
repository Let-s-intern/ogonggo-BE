package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ContentEventRemover internal constructor(
    private val contentEventRepository: ContentEventJpaRepository,
) {

    /**
     * 보관 기간이 지난 이벤트를 한 묶음([batchSize]건)만 물리 삭제하고 지운 수를 돌려준다.
     * 고치지 않는 기록이고 일별 집계가 숫자를 남기므로 소프트 삭제하지 않는다. 호출자가 0이 나올 때까지 반복한다.
     */
    fun removeExpired(today: LocalDate, batchSize: Int): Int {
        require(batchSize > 0) { "한 번에 지울 수는 1 이상이어야 합니다." }
        val ids = contentEventRepository.findIdsOccurredBefore(
            ContentEvent.retainedSince(today).atStartOfDay(),
            PageRequest.of(0, batchSize),
        )
        if (ids.isNotEmpty()) contentEventRepository.deleteAllByIdInBatch(ids)
        return ids.size
    }
}
