package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.persistence.BootcampMetricJpaRepository
import org.springframework.stereotype.Component

interface BootcampMetricReader {
    fun read(bootcampId: Long): BootcampMetricData

    /** 목록 조회의 N+1을 피하기 위해 한 번에 조회하며, 지표 행이 없는 부트캠프는 0으로 채운다. */
    fun readAll(bootcampIds: Collection<Long>): Map<Long, BootcampMetricData>
}

@Component
internal class BootcampMetricReaderImpl(
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
) : BootcampMetricReader {

    override fun read(bootcampId: Long): BootcampMetricData =
        bootcampMetricRepository.findByBootcampId(bootcampId)?.let(BootcampMetricData::from)
            ?: BootcampMetricData.EMPTY

    override fun readAll(bootcampIds: Collection<Long>): Map<Long, BootcampMetricData> {
        if (bootcampIds.isEmpty()) {
            return emptyMap()
        }

        val metrics = bootcampMetricRepository.findAllByBootcampIdIn(bootcampIds.toSet())
            .associateBy(BootcampMetric::bootcampId)

        return bootcampIds.associateWith { bootcampId ->
            metrics[bootcampId]?.let(BootcampMetricData::from) ?: BootcampMetricData.EMPTY
        }
    }
}

data class BootcampMetricData(
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        val EMPTY = BootcampMetricData(viewCount = 0, bookmarkCount = 0, commentCount = 0)

        internal fun from(metric: BootcampMetric): BootcampMetricData = BootcampMetricData(
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}
