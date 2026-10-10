package com.ogonggo.userapi.notification.intake.implement.signup

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration
import com.ogonggo.userapi.notification.intake.implement.signup.dto.SignUpAlimTalkPreparationDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 가입 커밋 이후에만 이벤트를 받아 적재를 전용 실행기에 넘긴다.
 * 가입 트랜잭션과 알림 적재는 분리되어 있으므로 적재 실패·큐 포화를 가입 실패로 전파하지 않는다.
 */
@Component
internal class UserSignUpAlimTalkEventListener(
    private val preparer: UserSignUpAlimTalkPreparer,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_ENQUEUE_TASK_EXECUTOR)
    private val enqueueTaskExecutor: TaskExecutor,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: UserSignedUpEvent) {
        enqueueAfterCommit(event)
    }

    private fun enqueueAfterCommit(event: UserSignedUpEvent) {
        try {
            // 별도 풀로 제출해 NHN 발송 지연이 가입 처리나 다른 백그라운드 작업을 막지 않게 한다.
            enqueueTaskExecutor.execute { appendNotification(event) }
        } catch (_: TaskRejectedException) {
            log.error("가입 알림 적재 실행기가 포화되어 알림을 건너뜁니다. userId={}, reason=EXECUTOR_REJECTED", event.userId)
        } catch (exception: Exception) {
            log.error(
                "가입 알림 적재 작업을 등록하지 못했습니다. userId={}, errorType={}",
                event.userId,
                exception::class.simpleName,
                safeStackTrace(exception),
            )
        }
    }

    private fun appendNotification(event: UserSignedUpEvent) {
        try {
            when (val preparation = preparer.prepare(event)) {
                is SignUpAlimTalkPreparationDto.Ready -> appendPreparedNotification(event, preparation)
                is SignUpAlimTalkPreparationDto.Skipped -> logSkipped(preparation)
            }
        } catch (exception: Exception) {
            log.error(
                "가입 알림을 적재하지 못했습니다. userId={}, errorType={}",
                event.userId,
                exception::class.simpleName,
                safeStackTrace(exception),
            )
        }
    }

    private fun appendPreparedNotification(
        event: UserSignedUpEvent,
        preparation: SignUpAlimTalkPreparationDto.Ready,
    ) {
        // AFTER_COMMIT 처리 중에도 별도 커밋을 보장하고, 중복 키 검사는 core Appender에 맡긴다.
        val inserted = notificationAppender.appendInNewTransaction(
            NotificationAppendDto(
                deduplicationKey = "${preparation.templateCode}:user:${event.userId}:KAKAO",
                channel = NotificationChannel.KAKAO,
                templateCode = preparation.templateCode,
                recipientAddress = preparation.recipientNo,
                payloadJson = objectMapper.writeValueAsString(preparation.templateParameters),
                scheduledAt = event.joinedAt,
                recipientUserId = event.userId,
            ),
        )
        log.info("가입 알림 적재 결과. result={}", if (inserted) "APPENDED" else "DUPLICATE")
    }

    private fun logSkipped(preparation: SignUpAlimTalkPreparationDto.Skipped) {
        log.info("가입 알림 적재를 생략했습니다. reason={}", preparation.reason)
    }

    private companion object {
        val log = LoggerFactory.getLogger(UserSignUpAlimTalkEventListener::class.java)
    }

    private fun safeStackTrace(exception: Exception) = RuntimeException("가입 알림 적재 실패").apply {
        // DB 예외 메시지에는 SQL 파라미터 등 수신자 정보가 포함될 수 있어 메시지와 cause는 버린다.
        stackTrace = exception.stackTrace
    }
}
