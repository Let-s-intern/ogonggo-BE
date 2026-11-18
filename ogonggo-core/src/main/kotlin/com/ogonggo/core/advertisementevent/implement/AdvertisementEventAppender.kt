package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementEventAppendDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/** 이벤트·고유 사용자·일별 집계를 한 트랜잭션에서 기록한다. 호출한 API Service가 트랜잭션을 연다. */
@Component
class AdvertisementEventAppender internal constructor(
    private val jdbcTemplate: JdbcTemplate,
) {

    /** 이미 처리한 같은 날 노출 이벤트는 여기서 끝낸다. 클릭은 반복 요청도 모두 기록한다. */
    fun append(dto: AdvertisementEventAppendDto) {
        // 계약이 없는 자체 홍보는 0으로 정규화해 계약별 유니크 키에서 NULL 중복 허용을 피한다.
        val contractKey = dto.contractId ?: NO_CONTRACT_KEY
        val deduplicationKey = dto.viewerKey.takeIf { dto.eventType == AdvertisementEventType.IMPRESSION }
        val jobRolesSnapshot = dto.jobRoles
            .takeIf { dto.eventType == AdvertisementEventType.CLICK && it.isNotEmpty() }
            ?.sortedBy { it.ordinal }
            ?.joinToString(",") { it.name }
        val now = dto.eventAt
        val insertedEvent = jdbcTemplate.update(
            INSERT_EVENT_SQL,
            dto.eventDate,
            dto.exposureId,
            dto.productId,
            dto.contractId,
            contractKey,
            dto.category.name,
            dto.position?.name,
            dto.targetType?.name,
            dto.targetId,
            dto.eventType.name,
            dto.eventAt,
            dto.memberId,
            dto.anonymousId,
            dto.viewerKey,
            deduplicationKey,
            now,
            now,
        )
        if (insertedEvent == 0) {
            return
        }

        val insertedViewer = jdbcTemplate.update(
            INSERT_DAILY_VIEWER_SQL,
            dto.eventDate,
            dto.exposureId,
            dto.productId,
            dto.contractId,
            contractKey,
            dto.category.name,
            dto.position?.name,
            dto.targetType?.name,
            dto.eventType.name,
            dto.viewerKey,
            dto.memberId,
            dto.anonymousId,
            dto.audienceType.name,
            jobRolesSnapshot,
            now,
            now,
        )

        jdbcTemplate.update(
            UPSERT_DAILY_METRIC_SQL,
            dto.eventDate,
            dto.exposureId,
            dto.productId,
            dto.contractId,
            contractKey,
            dto.category.name,
            dto.position?.name,
            dto.eventType.name,
            1,
            insertedViewer,
            now,
            now,
            1,
            insertedViewer,
            now,
        )
    }

    companion object {
        private const val NO_CONTRACT_KEY = 0L

        private const val INSERT_EVENT_SQL = """
            INSERT IGNORE INTO advertisement_events (
                event_date, exposure_id, product_id, contract_id, contract_key, category, position, target_type, target_id,
                event_type, event_at, member_id, anonymous_id, viewer_key, deduplication_key, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val INSERT_DAILY_VIEWER_SQL = """
            INSERT IGNORE INTO advertisement_event_daily_viewers (
                event_date, exposure_id, product_id, contract_id, contract_key, category, position, target_type, event_type,
                viewer_key, member_id, anonymous_id, audience_type, job_roles, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val UPSERT_DAILY_METRIC_SQL = """
            INSERT INTO advertisement_daily_metrics (
                event_date, exposure_id, product_id, contract_id, contract_key, category, position, event_type,
                total_count, unique_viewer_count, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_count = total_count + ?,
                unique_viewer_count = unique_viewer_count + ?,
                updated_at = ?
        """
    }
}
