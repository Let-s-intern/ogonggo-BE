package com.ogonggo.userapi.notification.alert

import java.time.LocalDateTime

enum class NotificationAlertType(
    val code: String,
    val title: String,
    val severity: String,
    val symptom: String,
    val impact: String,
    val action: String,
) {
    CLEANUP_BACKLOG(
        "A1", "알림 정리 적체", "주의",
        "정리 작업 이후에도 삭제 대상이 남아 누적되고 있습니다.",
        "발송 기록 누적으로 조회·삭제 비용과 저장 공간 사용량이 증가할 수 있습니다.",
        "정리 대상 잔량·최고 나이, 배치 소요시간, DB 잠금·실행 계획을 확인하세요.",
    ),
    CLEANUP_EXECUTION_FAILURE(
        "A3", "알림 정리 실패·실행 누락", "심각",
        "활성화된 정리 작업이 예정 시각에 실행되지 않았거나 실패했습니다.",
        "보관 기간을 지난 발송 기록이 정리되지 않습니다.",
        "scheduled_jobs의 cron·enabled, 마지막 시작·완료, 예외와 잠금 소유자를 확인하세요.",
    ),
    LOCK_EXPIRY_RISK(
        "B1", "ShedLock 만료 임박", "심각",
        "진행 중인 알림 작업의 실행 시간이 잠금 상한에 접근했습니다.",
        "잠금 만료 후 다른 인스턴스와 실행이 겹쳐 중복 발송될 수 있습니다.",
        "잠금 소유 작업의 진행, 외부 호출·DB 지연을 확인하세요. 잠금 시간만 늘리기 전에 원인을 확인하세요.",
    ),
    DELIVERY_STALLED(
        "B2", "알림 발송 진행 중단", "심각",
        "발송 가능한 대기 알림이 있지만 전체 인스턴스에서 처리 진행이 확인되지 않습니다.",
        "발송 기한 초과와 미발송으로 이어질 수 있습니다.",
        "잠금 획득·마지막 진행, 예약 풀, 발송·결과 DB 실행기를 확인하세요. 단일 인스턴스의 잠금 실패와 구분하세요.",
    ),
    PENDING_EXPIRED(
        "C2", "알림 발송 기한 초과", "심각",
        "새로 발송 기한을 넘긴 PENDING 알림이 발생했습니다.",
        "해당 알림은 자동 발송 대상에서 제외되며 DB에는 남습니다.",
        "채널별 대기량·처리량과 저장된 발송 기한을 확인하세요. 재발송 전 provider 접수 여부를 확인하세요.",
    ),
    DELIVERY_UNRESOLVED(
        "C3", "알림 발송 결과 기록 실패", "심각",
        "발송 결과를 DB에 기록하지 못했거나 처리 결과를 확정하지 못했습니다.",
        "외부 접수 후에도 PENDING으로 남아 다음 실행에서 재호출될 수 있습니다.",
        "UNRESOLVED 로그, 결과 DB 풀 거절·commit 오류를 확인하세요. 접수 여부 확인 없이 재발송하지 마세요.",
    ),
    ENQUEUE_FAILURE(
        "E1", "가입 알림 적재 실패", "심각",
        "가입 성공 후 알림 적재 또는 실행기 제출에 실패했습니다.",
        "알림 행이 생성되지 않아 대기량 지표에서도 유실이 보이지 않을 수 있습니다.",
        "적재 실행기 큐·거절 횟수와 DB 오류를 확인하세요. 누락 범위를 확인한 후 후속 처리를 결정하세요.",
    ),
    MONITORING_FAILURE(
        "E3", "알림 감시·경고 전달 이상", "심각",
        "지표 수집 또는 경고 전달 경로의 정상 동작을 확인하지 못했습니다.",
        "알림 장애가 발생해도 운영자가 경고를 받지 못할 수 있습니다.",
        "수집기 생존·마지막 표본, 연결·권한, Slack 응답을 확인하세요. 전체 감시기 정지는 외부 생존 감시로 확인하세요.",
    ),
}

enum class NotificationAlertStatus(val label: String) { FIRING("발생"), RESOLVED("복구"), TEST("TEST") }

/** measurements에는 건수·시간·오류 분류만 전달한다. 주소·토큰·본문·원본 예외는 넣지 않는다. */
data class NotificationAlert(
    val type: NotificationAlertType,
    val observedAt: LocalDateTime,
    val status: NotificationAlertStatus = NotificationAlertStatus.FIRING,
    val scope: String = "all",
    val measurements: Map<String, String> = emptyMap(),
) {
    init {
        require(scope.matches(Regex("[A-Za-z0-9_.:-]{1,64}"))) { "경고 범위는 64자 이내의 고정 분류여야 합니다." }
        require(measurements.size <= 6) { "경고 관측값은 6개 이하여야 합니다." }
    }
}
