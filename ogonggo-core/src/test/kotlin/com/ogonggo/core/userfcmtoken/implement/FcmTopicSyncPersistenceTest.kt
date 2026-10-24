package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.fcmtopicsynctask.implement.FcmTopicSyncTaskManager
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskQueryRepository
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastConfiguration
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest(showSql = false, properties = ["ogonggo.notification.broadcast.topic=ogonggo-all-test"])
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(UserFcmTokenManager::class, FcmTopicSyncTaskManager::class, FcmTopicSyncTaskQueryRepository::class,
    NotificationBroadcastConfiguration::class)
internal class FcmTopicSyncPersistenceTest @Autowired constructor(
    private val tokens: UserFcmTokenManager, private val tasks: FcmTopicSyncTaskManager,
    private val users: UserJpaRepository, private val tokenRows: UserFcmTokenJpaRepository,
    private val em: EntityManager,
) {
    @Test
    @DisplayName("토큰 교체와 해제는 이전 원문을 지우기 전에 구독 해제 작업을 남긴다")
    fun `토큰 변경과 구독 작업을 함께 저장한다`() {
        // given
        val id = user(801)
        tokens.register(id, UserFcmTokenRegisterDto("old", DEVICE), NOW)
        val initial = tasks.pending(NOW, 100).single()
        tasks.finish(initial, NOW)
        em.clear()
        // when
        tokens.register(id, UserFcmTokenRegisterDto("new", DEVICE), NOW)
        em.flush(); em.clear()
        // then
        val pending = tasks.pending(NOW, 100)
        assertEquals(mapOf("old" to false, "new" to true), pending.associate { it.token to it.subscribed })
        assertEquals("new", tokenRows.findAll().single().token)
        assertFalse(tasks.finish(initial, NOW))
    }

    @Test
    @DisplayName("오래된 구독 성공 결과는 이후 해제 요청을 완료 처리하지 못한다")
    fun `구독 결과는 요청 revision이 같을 때만 반영한다`() {
        // given
        val id = user(802)
        tokens.register(id, UserFcmTokenRegisterDto("token", DEVICE), NOW)
        val initial = tasks.pending(NOW, 100).single()
        // when
        tokens.revoke(id, "token", NOW)
        em.flush(); em.clear()
        val applied = tasks.finish(initial, NOW)
        // then
        assertFalse(applied)
        assertFalse(tasks.pending(NOW, 100).single().subscribed)
        assertNull(tokenRows.findAll().single().token)
    }

    @Test
    @DisplayName("실패한 구독은 지수 지연 뒤 재처리하고 성공하면 토큰 원문을 제거한다")
    fun `실패 재처리와 성공 후 원문 정리를 보장한다`() {
        // given
        tokens.register(user(803), UserFcmTokenRegisterDto("retry-token", DEVICE), NOW)
        val initial = tasks.pending(NOW, 100).single()
        // when
        tasks.retry(initial, "UNAVAILABLE", NOW)
        em.clear()
        val notYetDue = tasks.pending(NOW, 100)
        val due = tasks.pending(NOW.plusSeconds(5), 100).single()
        tasks.finish(due, NOW.plusSeconds(5))
        em.clear()
        // then
        assertTrue(notYetDue.isEmpty())
        assertEquals(1, due.attemptCount)
        assertEquals(0, tasks.countPending())
        val retained = em.createNativeQuery("select token from fcm_topic_sync_tasks").singleResult
        assertNull(retained)
    }

    @Test
    @DisplayName("기존 토큰 최초 구독은 중단 후 재실행해도 이미 기록한 토큰을 건너뛴다")
    fun `최초 구독을 작은 묶음으로 이어간다`() {
        // given
        val id = user(804)
        repeat(3) { tokenRows.saveAndFlush(UserFcmToken(id, UUID.randomUUID().toString(), "existing-$it", "slot-$it")) }
        // when
        assertEquals(2, tasks.reconcile(NOW, 2))
        em.flush(); em.clear()
        assertEquals(1, tasks.reconcile(NOW, 2))
        em.flush(); em.clear()
        // then
        assertEquals(0, tasks.reconcile(NOW, 2))
        assertEquals(setOf("existing-0", "existing-1", "existing-2"), tasks.pending(NOW, 100).map { it.token }.toSet())
    }

    @Test
    @DisplayName("회원 정지와 활성 복구는 기존 토큰의 구독 희망 상태에 반영된다")
    fun `회원 상태 변경을 주기적으로 반영한다`() {
        // given
        val id = user(805)
        tokens.register(id, UserFcmTokenRegisterDto("state-token", DEVICE), NOW)
        tasks.finish(tasks.pending(NOW, 100).single(), NOW)
        em.clear()
        em.createQuery("update User u set u.status = :status where u.id = :id")
            .setParameter("status", UserStatus.SUSPENDED).setParameter("id", id).executeUpdate()
        em.clear()
        // when
        tasks.reconcile(NOW, 100)
        em.flush(); em.clear()
        // then
        assertFalse(tasks.pending(NOW, 100).single().subscribed)
    }

    private fun user(externalId: Long) = checkNotNull(users.saveAndFlush(User.ofLetsCareer(externalId, NOW.minusDays(1))).id)
    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 24, 12, 0)
        private val DEVICE = UUID.fromString("330a7396-907a-4e7f-a127-0cf5a80565ca")
    }
}
