package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.persistence.BootcampMetricJpaRepository
import com.ogonggo.core.jpa.ensureRowCreated
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 지표 행 생성만 호출자와 분리된 트랜잭션에서 처리한다.
 *
 * 지표 행은 부트캠프 생성이 아니라 첫 지표 발생 시점에 만들어지므로 동시에 만들려는 요청이 겹칠 수 있다.
 * 그때 걸리는 유니크 제약 위반이 호출자의 트랜잭션까지 롤백 대상으로 만들면
 * 실패한 쪽은 상대가 만든 행을 읽지도, 이어서 갱신하지도 못한다.
 */
@Component
internal class BootcampMetricAppender(
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(bootcampId: Long) {
        bootcampMetricRepository.saveAndFlush(BootcampMetric(bootcampId = bootcampId))
    }
}

@Component
class BootcampMetricManager internal constructor(
    private val bootcampMetricRepository: BootcampMetricJpaRepository,
    private val bootcampMetricAppender: BootcampMetricAppender,
) {

    fun increaseViewCount(bootcampId: Long, now: LocalDateTime) {
        ensureMetric(bootcampId)
        bootcampMetricRepository.increaseViewCount(bootcampId, now)
    }

    /**
     * 북마크 수를 증감하지 않고 활성 북마크를 다시 세어 맞춘다.
     * 몇 번을 실행해도 결과가 같으므로 갱신을 한 번 놓쳐도 다음 갱신에서 값이 스스로 복구된다.
     */
    fun syncBookmarkCount(bootcampId: Long, now: LocalDateTime) {
        ensureMetric(bootcampId)
        bootcampMetricRepository.syncBookmarkCount(bootcampId, now)
    }

    private fun ensureMetric(bootcampId: Long) = ensureRowCreated(
        exists = { bootcampMetricRepository.findByBootcampId(bootcampId) != null },
        create = { bootcampMetricAppender.create(bootcampId) },
    )
}
