package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.persistence.BootcampBookmarkJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampMetricJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface BootcampMetricManager {
    fun increaseViewCount(bootcampId: Long, now: LocalDateTime)

    /**
     * 북마크 수를 증감하지 않고 활성 북마크를 다시 세어 맞춘다.
     * 몇 번을 실행해도 결과가 같으므로 갱신을 한 번 놓쳐도 다음 갱신에서 값이 스스로 복구된다.
     */
    fun syncBookmarkCount(bootcampId: Long)
}

@Component
internal class BootcampMetricManagerImpl(
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
    private val bootcampBookmarkRepository: BootcampBookmarkJpaRepository,
) : BootcampMetricManager {

    /**
     * 지표 행은 부트캠프 생성이 아니라 첫 지표 발생 시점에 만들어진다.
     * 같은 부트캠프를 동시에 조회하면 두 요청이 모두 행을 만들려 할 수 있으므로
     * 유니크 제약 위반을 확인한 뒤 증가 UPDATE로 되돌아간다.
     */
    override fun increaseViewCount(bootcampId: Long, now: LocalDateTime) {
        if (bootcampMetricRepository.increaseViewCount(bootcampId, now) > 0) {
            return
        }

        try {
            bootcampMetricRepository.saveAndFlush(BootcampMetric(bootcampId = bootcampId, viewCount = 1))
        } catch (exception: DataIntegrityViolationException) {
            bootcampMetricRepository.increaseViewCount(bootcampId, now)
        }
    }

    override fun syncBookmarkCount(bootcampId: Long) {
        val metric = bootcampMetricRepository.findByBootcampId(bootcampId)
            ?: BootcampMetric(bootcampId = bootcampId)
        metric.updateBookmarkCount(bootcampBookmarkRepository.countByBootcampIdAndDeletedAtIsNull(bootcampId))
        bootcampMetricRepository.save(metric)
    }
}
