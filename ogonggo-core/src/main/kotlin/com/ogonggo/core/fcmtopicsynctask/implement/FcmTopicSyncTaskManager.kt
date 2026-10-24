package com.ogonggo.core.fcmtopicsynctask.implement

import com.ogonggo.core.fcmtopicsynctask.domain.FcmTopicSyncTask
import com.ogonggo.core.fcmtopicsynctask.implement.dto.FcmTopicSyncTaskDto
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskJpaRepository
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskQueryRepository
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class FcmTopicSyncTaskManager internal constructor(
    private val repository: FcmTopicSyncTaskJpaRepository,
    private val queries: FcmTopicSyncTaskQueryRepository,
    private val properties: NotificationBroadcastProperties,
    private val tokens: UserFcmTokenJpaRepository,
    private val users: UserJpaRepository,
) {
    /** 토큰 변경 트랜잭션에서 원문을 비우기 전에 호출한다. */
    fun request(token: String, subscribed: Boolean, now: LocalDateTime) {
        if (properties.topic.isEmpty()) return
        val hash = UserFcmToken.hash(token)
        val existing = repository.findForUpdate(properties.topic, hash)
        if (existing == null) repository.saveAndFlush(FcmTopicSyncTask(properties.topic, hash, token, subscribed, now))
        else existing.request(token, subscribed, now)
    }

    fun reconcile(now: LocalDateTime, limit: Int): Int {
        val rows = queries.reconciliation(properties.topic, limit)
        rows.forEach { (userId, token) ->
            // 등록과 같은 회원 → 토큰 → 작업 잠금 순서로 오래된 최초 구독 후보를 재검증한다.
            val user = users.findForFcmTokenUpdate(userId) ?: return@forEach
            val current = tokens.findByHashForUpdate(UserFcmToken.hash(token)) ?: return@forEach
            if (current.userId == userId) request(token, user.status == UserStatus.ACTIVE, now)
        }
        return rows.size
    }

    fun pending(now: LocalDateTime, limit: Int) = queries.pending(properties.topic, now, limit)
    fun finish(task: FcmTopicSyncTaskDto, now: LocalDateTime) = queries.finish(task, now)
    fun retry(task: FcmTopicSyncTaskDto, code: String, now: LocalDateTime) = queries.retry(task, code, now)
    fun countPending() = queries.countPending(properties.topic)
}
