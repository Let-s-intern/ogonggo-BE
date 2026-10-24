package com.ogonggo.userapi.notification.alert

import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackText
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
internal class NotificationAlertTemplate(private val properties: NotificationAlertProperties) {
    fun render(alert: NotificationAlert): SlackMessage {
        val severity = if (alert.status == NotificationAlertStatus.RESOLVED) "정상" else alert.type.severity
        val headline = "[${alert.status.label}][$severity][${SlackText.escape(properties.environment)}] " +
            "오공고 알림 · ${alert.type.title} (${alert.type.code})"
        val description = when (alert.status) {
            NotificationAlertStatus.FIRING -> alert.type.symptom
            NotificationAlertStatus.RESOLVED -> "감지 조건이 해소되었습니다. 아래 관측값과 후속 처리를 확인하세요."
            NotificationAlertStatus.TEST -> "연동·템플릿 검증용 샘플입니다. 실제 장애나 운영 측정값이 아닙니다.\n${alert.type.symptom}"
        }
        val blocks = mutableListOf(
            SlackBlock.text(SlackText(text = description)),
            SlackBlock.fields(listOf(
                SlackText.labeled("관측 시각 (Asia/Seoul)", alert.observedAt.format(TIME_FORMAT)),
                SlackText.labeled("인스턴스", properties.instanceId),
                SlackText.labeled("경고 유형", alert.type.name),
                SlackText.labeled("범위", alert.scope),
            )),
        )
        if (alert.measurements.isNotEmpty()) {
            blocks += SlackBlock.fields(alert.measurements.map { (name, value) ->
                // & 같은 문자는 이스케이프 후 길이가 늘어나므로 Slack field의 2,000자 안에 여유를 둔다.
                SlackText.labeled(name.take(60), value.take(300))
            })
        }
        blocks += SlackBlock.text(SlackText.labeled("영향", alert.type.impact))
        blocks += SlackBlock.text(SlackText.labeled("확인할 항목", alert.type.action))
        return SlackMessage.withHeadline(headline, blocks)
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
