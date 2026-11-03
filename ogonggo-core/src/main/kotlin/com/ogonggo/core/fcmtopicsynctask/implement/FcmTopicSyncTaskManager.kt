package com.ogonggo.core.fcmtopicsynctask.implement

import com.ogonggo.core.fcmtopicsynctask.domain.FcmTopicSyncTask
import com.ogonggo.core.fcmtopicsynctask.implement.dto.FcmTopicSyncTaskDto
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskJpaRepository
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskQueryRepository
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 설정된 토픽(전체·일반회원·기업회원)마다 토큰이 구독돼 있어야 하는지를 `fcm_topic_sync_tasks`에 기록한다.
 * 실제 FCM 구독·해제 요청은 사용자 API의 구독 동기화 작업이 이 기록을 읽어 보낸다.
 */
@Component
class FcmTopicSyncTaskManager internal constructor(
    private val repository: FcmTopicSyncTaskJpaRepository,
    private val queries: FcmTopicSyncTaskQueryRepository,
    private val properties: NotificationBroadcastProperties,
    private val tokens: UserFcmTokenJpaRepository,
    private val users: UserJpaRepository,
) {
    /** 토큰 등록 트랜잭션에서 호출한다. 회원 역할·상태에 따라 토픽마다 구독 또는 해제를 요청한다. */
    fun requestMembership(token: String, role: UserRole, status: UserStatus, now: LocalDateTime) {
        val hash = UserFcmToken.hash(token)
        properties.topicAudiences().forEach { (topic, audience) ->
            request(topic, hash, token, audience.includes(role, status), now)
        }
    }

    /** 토큰 해제·교체·계정 전환 트랜잭션에서 원문을 비우기 전에 호출한다. 모든 토픽에서 해제를 요청한다. */
    fun requestUnsubscribeAll(token: String, now: LocalDateTime) {
        val hash = UserFcmToken.hash(token)
        properties.syncTopics().forEach { topic -> request(topic, hash, token, false, now) }
    }

    /**
     * 구독 작업이 없는데 구독해야 하거나 희망 상태가 회원 정보와 어긋난 토큰을 토픽마다 찾아 합친다. 트랜잭션 없이 읽는다.
     * 찾은 후보는 [reconcile]이 다시 잠그고 확인한 뒤 고친다.
     */
    fun reconciliationCandidates(limit: Int): List<Pair<Long, String>> {
        val candidates = LinkedHashMap<String, Pair<Long, String>>()
        for ((topic, audience) in properties.topicAudiences()) {
            if (candidates.size >= limit) break
            queries.reconciliation(topic, audience, limit).forEach { candidates.putIfAbsent(it.second, it) }
        }
        return candidates.values.take(limit)
    }

    /**
     * 호출자 트랜잭션에서 후보를 고친다. 후보 수만큼 엔티티가 영속성 컨텍스트에 쌓이므로 호출자는 작은 묶음으로 나눠 부른다.
     * 등록과 같은 회원 → 토큰 → 작업 잠금 순서로 다시 확인한다.
     */
    fun reconcile(candidates: List<Pair<Long, String>>, now: LocalDateTime): Int {
        candidates.forEach { (userId, token) ->
            val user = users.findForFcmTokenUpdate(userId) ?: return@forEach
            val current = tokens.findByHashForUpdate(UserFcmToken.hash(token)) ?: return@forEach
            if (current.userId == userId) requestMembership(token, user.role, user.status, now)
        }
        return candidates.size
    }

    fun pending(now: LocalDateTime, limit: Int) = queries.pending(properties.syncTopics(), now, limit)
    fun finish(task: FcmTopicSyncTaskDto, now: LocalDateTime) = queries.finish(task, now)
    fun retry(task: FcmTopicSyncTaskDto, code: String, now: LocalDateTime) = queries.retry(task, code, now)
    fun countPending() = queries.countPending(properties.syncTopics())

    private fun request(topic: String, hash: String, token: String, subscribed: Boolean, now: LocalDateTime) {
        val existing = repository.findForUpdate(topic, hash)
        when {
            existing != null -> existing.request(token, subscribed, now)
            // 작업이 없으면 이 토픽에 구독시킨 적이 없으므로 해제 요청을 만들지 않는다. 기업회원 기기에 일반회원 토픽 해제를 보내는 일을 막는다.
            subscribed -> repository.saveAndFlush(FcmTopicSyncTask(topic, hash, token, true, now))
        }
    }
}
