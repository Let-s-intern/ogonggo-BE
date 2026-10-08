package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.persistence.ConcernMetricJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/** 지표 행은 고민글을 등록할 때 [ConcernAppender]가 만든다. 여기서는 갱신만 한다. */
@Component
class ConcernMetricManager internal constructor(
    private val concernMetricRepository: ConcernMetricJpaRepository,
) {

    fun increaseViewCount(concernId: Long, now: LocalDateTime) {
        check(concernMetricRepository.increaseViewCount(concernId, now) > 0) {
            "고민글 지표 행을 갱신하지 못했습니다. concernId=$concernId"
        }
    }

    fun increaseCommentCount(concernId: Long, now: LocalDateTime) {
        check(concernMetricRepository.increaseCommentCount(concernId, now) > 0) {
            "고민글 지표 행을 갱신하지 못했습니다. concernId=$concernId"
        }
    }

    /** 지표는 화면 표시용이라, 값이 이미 0이어서 줄이지 못해도 답변 삭제를 실패시키지 않는다. */
    fun decreaseCommentCount(concernId: Long, now: LocalDateTime) {
        if (concernMetricRepository.decreaseCommentCount(concernId, now) > 0) return
        log.warn("답변 수가 이미 0이거나 지표 행이 없어 줄이지 않았습니다. concernId={}", concernId)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ConcernMetricManager::class.java)
    }
}
