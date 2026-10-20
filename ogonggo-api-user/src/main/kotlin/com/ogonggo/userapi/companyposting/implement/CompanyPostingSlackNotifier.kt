package com.ogonggo.userapi.companyposting.implement

import com.ogonggo.userapi.companyposting.business.CompanyPostingRegisteredEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.COMPANY_POSTING_TASK_EXECUTOR
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackProperties
import com.ogonggo.userapi.slack.SlackText
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.client.RestClient
import java.time.format.DateTimeFormatter

/**
 * 기업회원이 등록한 채용공고·부트캠프를 운영 채널에 알린다.
 *
 * 커밋된 뒤에만 보낸다. 등록이 롤백됐는데 알림만 나가면 없는 공고를 검수하러 가게 된다.
 * 슬랙이 느리거나 실패해도 등록 응답은 영향받지 않도록 별도 실행기에서 보내고 예외를 삼킨다.
 * 공고는 DB에 남아 있어 알림 하나를 놓쳐도 어드민에서 찾을 수 있다.
 */
@Component
internal class CompanyPostingSlackNotifier(
    @Qualifier(SLACK_REST_CLIENT)
    private val slackRestClient: RestClient,
    private val properties: SlackProperties,
) {

    @Async(COMPANY_POSTING_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun notify(event: CompanyPostingRegisteredEvent) {
        if (properties.companyPosting.isBlank()) {
            log.warn(
                "ogonggo.slack.company-posting이 설정되지 않아 알림을 건너뜁니다. type={}, postingId={}",
                event.type,
                event.postingId,
            )
            return
        }

        try {
            slackRestClient.post()
                .uri(properties.companyPosting)
                .body(event.toSlackMessage())
                .retrieve()
                .toBodilessEntity()
        } catch (exception: Exception) {
            log.warn(
                "기업회원 등록 슬랙 알림 발송에 실패했습니다. type={}, postingId={}",
                event.type,
                event.postingId,
                exception,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CompanyPostingSlackNotifier::class.java)
    }
}

private val RECRUITMENT_END_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 요약 줄에 종류·기업명·제목을 넣어 채널 목록에서 바로 어떤 건인지 보이게 한다. */
private fun CompanyPostingRegisteredEvent.toSlackMessage(): SlackMessage =
    SlackMessage(
        text = "🆕 비즈니스 ${type.desc} 등록 - ${SlackText.escape(companyName)} · ${SlackText.escape(title)}",
        blocks = listOf(
            SlackBlock.fields(
                listOf(
                    SlackText.labeled("종류", type.desc),
                    SlackText.labeled("ID", postingId.toString()),
                    SlackText.labeled("기업명", companyName),
                    SlackText.labeled("등록 계정", accountEmail),
                    SlackText.labeled("모집 마감", recruitmentEndAt?.format(RECRUITMENT_END_AT_FORMAT)),
                ),
            ),
            SlackBlock.text(SlackText.labeled("제목", title)),
        ),
    )
