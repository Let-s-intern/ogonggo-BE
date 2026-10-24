package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryRecordDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationResultReader internal constructor(
    private val notificationRepository: NotificationJpaRepository,
) {
    /** 운영 화면용 최신 결과 목록이며 주소·payload는 읽지 않는다. */
    @Transactional(readOnly = true)
    fun readRecent(limit: Int): List<NotificationDeliveryRecordDto> {
        require(limit in 1..MAX_RESULT_COUNT) { "최근 결과 조회 건수는 1~$MAX_RESULT_COUNT 범위여야 합니다." }
        return notificationRepository.findRecentResults(PageRequest.of(0, limit))
    }

    private companion object {
        const val MAX_RESULT_COUNT = 100
    }
}
