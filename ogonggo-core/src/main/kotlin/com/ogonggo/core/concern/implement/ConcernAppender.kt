package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernMetric
import com.ogonggo.core.concern.implement.dto.ConcernAppendDto
import com.ogonggo.core.concern.persistence.ConcernJpaRepository
import com.ogonggo.core.concern.persistence.ConcernMetricJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernAppender internal constructor(
    private val concernRepository: ConcernJpaRepository,
    private val concernMetricRepository: ConcernMetricJpaRepository,
) {

    /** 지표 행을 함께 만들어 조회 수·답변 수 갱신이 행 생성을 신경 쓰지 않게 한다. 호출자의 트랜잭션에서 실행한다. */
    fun append(dto: ConcernAppendDto): Concern {
        val concern = concernRepository.save(dto.toEntity())
        concernMetricRepository.save(ConcernMetric(concernId = checkNotNull(concern.id) { "저장된 고민글 식별자가 없습니다." }))
        return concern
    }
}
