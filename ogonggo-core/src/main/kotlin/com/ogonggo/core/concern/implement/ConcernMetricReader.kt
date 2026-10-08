package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernMetric
import com.ogonggo.core.concern.implement.dto.ConcernMetricDto
import com.ogonggo.core.concern.persistence.ConcernMetricJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernMetricReader internal constructor(
    private val concernMetricRepository: ConcernMetricJpaRepository,
) {

    fun read(concernId: Long): ConcernMetricDto =
        concernMetricRepository.findByConcernId(concernId)?.let(ConcernMetricDto::from) ?: ConcernMetricDto.EMPTY

    fun readAll(concernIds: Collection<Long>): Map<Long, ConcernMetricDto> {
        if (concernIds.isEmpty()) return emptyMap()

        val metrics = concernMetricRepository.findAllByConcernIdIn(concernIds.toSet())
            .associateBy(ConcernMetric::concernId)
        return concernIds.associateWith { metrics[it]?.let(ConcernMetricDto::from) ?: ConcernMetricDto.EMPTY }
    }
}
