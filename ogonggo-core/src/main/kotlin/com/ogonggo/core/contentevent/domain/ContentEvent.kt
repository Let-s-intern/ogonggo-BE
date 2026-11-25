package com.ogonggo.core.contentevent.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 브라우저에서 일어난 콘텐츠 이벤트 한 건이다. 광고 성과 리포트가 이 기록을 날짜별로 모아 센다.
 *
 * 일어난 사실의 기록이라 고치지 않으며 소프트 삭제·수정 일시 칼럼을 두지 않는다.
 * 보관 기간([RETENTION_DAYS]일)이 지나면 물리 삭제하고 일별 집계(`content_daily_stats`)만 남긴다.
 * 같은 방문자가 1분 안에 다시 연 조회도 모두 저장하고, 한 번으로 세는 것은 집계가 한다.
 * [occurredAt]은 서버가 받은 시각(KST)이다. 브라우저 시각은 믿지 않는다.
 */
@Entity
@Table(
    name = "content_events",
    indexes = [
        Index(name = "idx_content_events_target_occurred_at", columnList = "target_type, target_id, occurred_at"),
        Index(name = "idx_content_events_exposure_occurred_at", columnList = "exposure_id, occurred_at"),
        Index(name = "idx_content_events_occurred_at", columnList = "occurred_at"),
        Index(name = "idx_content_events_visitor_occurred_at", columnList = "visitor_id, occurred_at"),
    ],
)
internal class ContentEvent(
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: ContentEventType, /* 이벤트 종류 */

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    val targetType: ContentEventTargetType?, /* 대상 콘텐츠 종류. 대상 없는 배너 이벤트면 없다 */

    @Column(name = "target_id")
    val targetId: Long?, /* 대상 채용공고·부트캠프·취준고민·모집글 식별자 */

    @Column(name = "exposure_id")
    val exposureId: Long?, /* 광고 노출 식별자. 광고로 들어오지 않았으면 없다 */

    @Column(name = "user_id")
    val userId: Long?, /* 로그인한 회원 식별자. 비회원이면 없다 */

    @Column(name = "visitor_id", nullable = false, length = MAX_VISITOR_ID_LENGTH)
    val visitorId: String, /* 브라우저의 익명 방문자 식별자 */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: ContentEventSource, /* 유입 경로 */

    @Column(name = "scroll_percent")
    val scrollPercent: Int?, /* 가장 깊이 내린 스크롤 위치(0~100). 스크롤 이벤트만 */

    @Column(name = "duration_ms")
    val durationMs: Long?, /* 화면이 보인 시간(밀리초). 스크롤 이벤트만 */

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: LocalDateTime, /* 서버가 받은 시각 */
) {

    init {
        require((targetType == null) == (targetId == null)) { "대상 종류와 식별자는 함께 있어야 합니다." }
        require(targetId == null || targetId > 0) { "대상 식별자는 양수여야 합니다." }
        require(!eventType.requiresTarget || targetId != null) { "${eventType.name} 이벤트는 대상이 있어야 합니다." }
        require(exposureId == null || exposureId > 0) { "광고 노출 식별자는 양수여야 합니다." }
        require(!eventType.requiresExposure || exposureId != null) { "${eventType.name} 이벤트는 광고 노출이 있어야 합니다." }
        require(userId == null || userId > 0) { "회원 식별자는 양수여야 합니다." }
        require(visitorId.isNotBlank() && visitorId.length <= MAX_VISITOR_ID_LENGTH) {
            "방문자 식별자는 ${MAX_VISITOR_ID_LENGTH}자 이하여야 합니다."
        }
        require(scrollPercent == null || scrollPercent in 0..MAX_SCROLL_PERCENT) { "스크롤 위치는 0~100이어야 합니다." }
        require(durationMs == null || durationMs >= 0) { "보인 시간은 0 이상이어야 합니다." }
        require(eventType == ContentEventType.SCROLL || (scrollPercent == null && durationMs == null)) {
            "스크롤 위치와 보인 시간은 스크롤 이벤트에만 있습니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 콘텐츠 이벤트 식별자 */
        protected set

    companion object {
        const val MAX_VISITOR_ID_LENGTH = 36
        const val MAX_SCROLL_PERCENT = 100

        /** 원본 보관 기간(일)이다. 오늘에서 이 날수를 뺀 날의 0시 앞 이벤트를 지운다. */
        const val RETENTION_DAYS = 365L

        /** 원본이 모두 남아 있는 첫날이다. 이날부터는 기간 순 방문자를 원본에서 셀 수 있다. */
        fun retainedSince(today: LocalDate): LocalDate = today.minusDays(RETENTION_DAYS)
    }
}
