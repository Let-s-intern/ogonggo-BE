package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.implement.dto.BootcampMetricDto
import com.ogonggo.core.bootcamp.persistence.BootcampMetricJpaRepository
import org.springframework.stereotype.Component

@Component
class BootcampMetricReader internal constructor(
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
) {

    fun read(bootcampId: Long): BootcampMetricDto =
        bootcampMetricRepository.findByBootcampId(bootcampId)?.let(BootcampMetricDto::from)
            ?: BootcampMetricDto.EMPTY

    /** 목록 조회의 N+1을 피하기 위해 한 번에 조회하며, 지표 행이 없는 부트캠프는 0으로 채운다. */

    fun readAll(bootcampIds: Collection<Long>): Map<Long, BootcampMetricDto> {
        if (bootcampIds.isEmpty()) {
            return emptyMap()
        }

        val metrics = bootcampMetricRepository.findAllByBootcampIdIn(bootcampIds.toSet())
            .associateBy(BootcampMetric::bootcampId)

        return bootcampIds.associateWith { bootcampId ->
            metrics[bootcampId]?.let(BootcampMetricDto::from) ?: BootcampMetricDto.EMPTY
        }
    }
}
