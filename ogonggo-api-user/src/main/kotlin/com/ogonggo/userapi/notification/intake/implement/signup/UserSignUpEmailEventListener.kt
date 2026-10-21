package com.ogonggo.userapi.notification.intake.implement.signup

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration
import com.ogonggo.userapi.notification.channel.email.EmailTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 일반 회원의 오공고 계정이 처음 만들어지면 가입 환영 메일을 적재한다.
 * 기업 회원 가입은 이 이벤트를 발행하지 않으므로 대상이 아니다.
 *
 * 가입 알림톡과 같은 이벤트를 받지만 listener를 나눴다. 한 채널의 적재 실패나 생략이 다른 채널에 영향을 주지 않게 하기 위해서다.
 * 가입 커밋 이후에만 이벤트를 받아 적재를 전용 실행기에 넘기며, 적재 실패·큐 포화를 가입 실패로 전파하지 않는다.
 */
@Component
internal class UserSignUpEmailEventListener(
    private val notificationAppender: NotificationAppender,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_ENQUEUE_TASK_EXECUTOR)
    private val enqueueTaskExecutor: TaskExecutor,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: UserSignedUpEvent) {
        try {
            enqueueTaskExecutor.execute { appendNotification(event) }
        } catch (_: TaskRejectedException) {
            log.error("가입 환영 메일 적재 실행기가 포화되어 메일을 건너뜁니다. userId={}, reason=EXECUTOR_REJECTED", event.userId)
        } catch (exception: Exception) {
            log.error(
                "가입 환영 메일 적재 작업을 등록하지 못했습니다. userId={}, errorType={}",
                event.userId,
                exception::class.simpleName,
                safeStackTrace(exception),
            )
        }
    }

    private fun appendNotification(event: UserSignedUpEvent) {
        // 수신 주소는 렛츠커리어 가입 이메일이다. 원문은 로그에 남기지 않고 생략 사유만 남긴다.
        val email = event.email?.takeIf(String::isNotBlank)
        if (email == null) {
            log.info("가입 환영 메일 적재를 생략했습니다. reason=MISSING_EMAIL")
            return
        }

        try {
            // AFTER_COMMIT 처리 중에도 별도 커밋을 보장하고, 중복 키 검사는 core Appender에 맡긴다.
            val inserted = notificationAppender.appendInNewTransaction(
                NotificationAppendDto(
                    deduplicationKey = "${TEMPLATE.templateCode}:user:${event.userId}:EMAIL",
                    channel = NotificationChannel.EMAIL,
                    templateCode = TEMPLATE.templateCode,
                    recipientAddress = email,
                    // 환영 메일은 모든 회원에게 같은 본문을 보내므로 템플릿 변수가 없다.
                    payloadJson = EMPTY_PAYLOAD,
                    scheduledAt = event.joinedAt,
                    recipientUserId = event.userId,
                ),
            )
            log.info("가입 환영 메일 적재 결과. result={}", if (inserted) "APPENDED" else "DUPLICATE")
        } catch (exception: Exception) {
            log.error(
                "가입 환영 메일을 적재하지 못했습니다. userId={}, errorType={}",
                event.userId,
                exception::class.simpleName,
                safeStackTrace(exception),
            )
        }
    }

    private fun safeStackTrace(exception: Exception) = RuntimeException("가입 환영 메일 적재 실패").apply {
        // DB 예외 메시지에는 SQL 파라미터 등 수신자 정보가 포함될 수 있어 메시지와 cause는 버린다.
        stackTrace = exception.stackTrace
    }

    private companion object {
        val TEMPLATE = EmailTemplate.SIGN_UP_WELCOME
        const val EMPTY_PAYLOAD = "{}"
        val log = LoggerFactory.getLogger(UserSignUpEmailEventListener::class.java)
    }
}
