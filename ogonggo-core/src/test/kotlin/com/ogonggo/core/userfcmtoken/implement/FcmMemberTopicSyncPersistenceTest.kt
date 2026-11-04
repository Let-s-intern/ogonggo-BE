package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.fcmtopicsynctask.implement.FcmTopicSyncTaskManager
import com.ogonggo.core.fcmtopicsynctask.persistence.FcmTopicSyncTaskQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastConfiguration
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.util.UUID

/** 전체·일반회원·기업회원 토픽이 모두 설정됐을 때, 토큰마다 회원 종류에 맞는 토픽 구독 작업이 생기는지 확인한다. */
@DataJpaTest(
    showSql = false,
    properties = [
        "ogonggo.notification.broadcast.topic=$ALL_TOPIC",
        "ogonggo.notification.broadcast.general-topic=$GENERAL_TOPIC",
        "ogonggo.notification.broadcast.company-topic=$COMPANY_TOPIC",
    ],
)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(UserFcmTokenManager::class, FcmTopicSyncTaskManager::class, FcmTopicSyncTaskQueryRepository::class,
    NotificationBroadcastConfiguration::class)
internal class FcmMemberTopicSyncPersistenceTest @Autowired constructor(
    private val tokens: UserFcmTokenManager, private val tasks: FcmTopicSyncTaskManager,
    private val users: UserJpaRepository, private val tokenRows: UserFcmTokenJpaRepository,
    private val em: EntityManager,
) {
    @Test
    @DisplayName("일반 회원이 토큰을 등록하면 전체·일반회원 토픽 구독 요청만 생기고 기업회원 토픽 요청은 생기지 않는다")
    fun `일반 회원 토큰은 전체와 일반회원 토픽에 구독한다`() {
        // given
        val id = generalMember(901)
        // when
        tokens.register(id, UserFcmTokenRegisterDto("general-token", DEVICE), NOW)
        em.flush(); em.clear()
        // then
        assertEquals(mapOf(ALL_TOPIC to true, GENERAL_TOPIC to true), pendingByTopic())
    }

    @Test
    @DisplayName("기업 회원이 토큰을 등록하면 전체·기업회원 토픽 구독 요청만 생긴다")
    fun `기업 회원 토큰은 전체와 기업회원 토픽에 구독한다`() {
        // given
        val id = companyMember("company@example.com")
        // when
        tokens.register(id, UserFcmTokenRegisterDto("company-token", DEVICE), NOW)
        em.flush(); em.clear()
        // then
        assertEquals(mapOf(ALL_TOPIC to true, COMPANY_TOPIC to true), pendingByTopic())
    }

    @Test
    @DisplayName("토큰을 해제하면 구독돼 있던 모든 토픽에 해제 요청이 생긴다")
    fun `토큰 해제는 구독한 모든 토픽에서 해제한다`() {
        // given
        val id = generalMember(902)
        tokens.register(id, UserFcmTokenRegisterDto("revoke-token", DEVICE), NOW)
        tasks.pending(NOW, 100).forEach { tasks.finish(it, NOW) }
        em.clear()
        // when
        tokens.revoke(id, "revoke-token", NOW)
        em.flush(); em.clear()
        // then
        assertEquals(mapOf(ALL_TOPIC to false, GENERAL_TOPIC to false), pendingByTopic())
    }

    @Test
    @DisplayName("토픽 설정 전에 등록한 토큰은 재평가로 회원 종류에 맞는 토픽에 구독 요청이 생긴다")
    fun `기존 토큰은 재평가로 회원 종류 토픽에 구독한다`() {
        // given
        val general = generalMember(903)
        val company = companyMember("existing@example.com")
        tokenRows.saveAndFlush(UserFcmToken(general, UUID.randomUUID().toString(), "existing-general", "slot-general"))
        tokenRows.saveAndFlush(UserFcmToken(company, UUID.randomUUID().toString(), "existing-company", "slot-company"))
        // when
        tasks.reconcile(tasks.reconciliationCandidates(100), NOW)
        em.flush(); em.clear()
        // then
        val pending = tasks.pending(NOW, 100).map { it.token to it.topic }.toSet()
        assertEquals(setOf(
            "existing-general" to ALL_TOPIC, "existing-general" to GENERAL_TOPIC,
            "existing-company" to ALL_TOPIC, "existing-company" to COMPANY_TOPIC,
        ), pending)
    }

    @Test
    @DisplayName("회원이 정지되면 재평가로 전체·회원 종류 토픽 모두 해제 요청이 생긴다")
    fun `정지된 회원의 토큰은 모든 토픽에서 해제한다`() {
        // given
        val id = companyMember("suspended@example.com")
        tokens.register(id, UserFcmTokenRegisterDto("suspended-token", DEVICE), NOW)
        tasks.pending(NOW, 100).forEach { tasks.finish(it, NOW) }
        em.clear()
        em.createQuery("update User u set u.status = :status where u.id = :id")
            .setParameter("status", UserStatus.SUSPENDED).setParameter("id", id).executeUpdate()
        em.clear()
        // when
        tasks.reconcile(tasks.reconciliationCandidates(100), NOW)
        em.flush(); em.clear()
        // then
        assertEquals(mapOf(ALL_TOPIC to false, COMPANY_TOPIC to false), pendingByTopic())
    }

    private fun pendingByTopic() = tasks.pending(NOW, 100).associate { it.topic to it.subscribed }

    private fun generalMember(externalId: Long) =
        checkNotNull(users.saveAndFlush(User.ofLetsCareer(externalId, NOW.minusDays(1))).id)

    private fun companyMember(email: String) =
        checkNotNull(users.saveAndFlush(User.ofCompany(email, "encoded-password", NOW.minusDays(1))).id)

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 31, 12, 0)
        val DEVICE: UUID = UUID.fromString("440a7396-907a-4e7f-a127-0cf5a80565ca")
    }
}

private const val ALL_TOPIC = "ogonggo-all-test"
private const val GENERAL_TOPIC = "ogonggo-general-test"
private const val COMPANY_TOPIC = "ogonggo-company-test"
