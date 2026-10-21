package com.ogonggo.core.userfcmtoken.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.userfcmtoken.domain.UserFcmToken
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.userfcmtoken.persistence.UserFcmTokenJpaRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(UserFcmTokenManager::class, UserFcmTokenReader::class, NotificationAppender::class)
internal class UserFcmTokenPersistenceTest @Autowired constructor(
    private val manager: UserFcmTokenManager,
    private val reader: UserFcmTokenReader,
    private val tokens: UserFcmTokenJpaRepository,
    private val users: UserJpaRepository,
    private val appender: NotificationAppender,
    private val notifications: NotificationJpaRepository,
    private val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Test
    fun `한 설치의 갱신과 해제가 다른 설치를 변경하지 않고 토큰 원문과 활성 키를 비운다`() {
        // given
        val userId = user(101)
        register(userId, "desktop", DESKTOP)
        register(userId, "mobile", MOBILE)
        val desktopId = reader.readActive(userId).single { it.token == "desktop" }.id
        // when
        register(userId, "desktop-new", DESKTOP)
        manager.revoke(userId, "mobile", NOW)
        entityManager.clear()
        // then
        assertEquals(desktopId, reader.readActive(userId).single().id)
        assertEquals("desktop-new", reader.readLatest(userId))
        val revoked = tokens.findAll().single { it.deviceId == MOBILE.toString() }
        assertEquals(NOW, revoked.deletedAt)
        assertNull(revoked.token)
        assertNull(revoked.tokenHash)
        assertNull(revoked.activeSlot)
    }

    @Test
    fun `기존 클라이언트 슬롯 교체는 UUID로 등록한 설치를 유지한다`() {
        // given
        val userId = user(102)
        register(userId, "desktop", DESKTOP)
        register(userId, "legacy-old")
        // when
        register(userId, "legacy-new")
        // then
        assertEquals(setOf("desktop", "legacy-new"), reader.readActive(userId).map { it.token }.toSet())
        assertEquals(2, tokens.findAll().count { it.userId == userId })
    }

    @Test
    fun `같은 토큰을 다른 계정에 등록하면 이전 소유자는 해제할 수 없다`() {
        // given
        val previous = user(103)
        val current = user(104)
        register(previous, "shared-token", DESKTOP)
        // when
        register(current, "shared-token", MOBILE)
        manager.revoke(previous, "shared-token", NOW)
        // then
        assertTrue(reader.readActive(previous).isEmpty())
        assertEquals("shared-token", reader.readLatest(current))
        assertFalse(reader.isDeliverable(previous, "shared-token"))
        assertTrue(reader.isDeliverable(current, "shared-token"))
    }

    @Test
    fun `같은 설치에서 토큰과 계정이 함께 바뀌어도 이전 계정 연결을 해제한다`() {
        // given
        val previous = user(105)
        val current = user(106)
        register(previous, "old", DESKTOP)
        // when
        register(current, "new", DESKTOP)
        // then
        assertTrue(reader.readActive(previous).isEmpty())
        assertEquals("new", reader.readLatest(current))
        assertEquals(1, tokens.findAll().count { it.activeSlot == DESKTOP.toString() })
    }

    @Test
    fun `같은 토큰 반복 등록은 한 행이고 슬롯 이동 후에도 활성 연결은 한 개다`() {
        // given
        val userId = user(107)
        register(userId, "same", DESKTOP)
        register(userId, "same", DESKTOP)
        assertEquals(1, tokens.findAll().count { it.userId == userId })
        // when
        register(userId, "same", MOBILE)
        // then
        assertEquals(1, reader.readActive(userId).size)
        assertEquals(MOBILE.toString(), tokens.findAll().single { it.userId == userId && it.deletedAt == null }.deviceId)
    }

    @Test
    fun `이전 토큰의 지연된 무효 응답과 반복 해제는 새 토큰을 지우지 않는다`() {
        // given
        val userId = user(108)
        register(userId, "old", DESKTOP)
        register(userId, "new", DESKTOP)
        // when
        repeat(2) { manager.revoke(userId, "old", NOW) }
        // then
        assertEquals("new", reader.readLatest(userId))
        repeat(2) { manager.revoke(userId, "new", NOW) }
        assertNull(reader.readLatest(userId))
        register(userId, "new", DESKTOP)
        assertEquals("new", reader.readLatest(userId))
    }

    @Test
    fun `발송 가능 여부는 현재 회원 상태와 토큰 유효성을 함께 판단한다`() {
        // given
        val userId = user(109)
        register(userId, "token", DESKTOP)
        assertTrue(reader.isDeliverable(userId, "token"))
        assertFalse(reader.isDeliverable(userId, "unknown"))
        // when
        val user = users.findById(userId).orElseThrow()
        user.suspend()
        users.flush()
        // then
        assertFalse(reader.isDeliverable(userId, "token"))
        user.withdraw(NOW)
        users.flush()
        assertFalse(reader.isDeliverable(userId, "token"))
        assertFalse(reader.isDeliverable(Long.MAX_VALUE, "token"))
    }

    @Test
    fun `최근 토큰 조회는 갱신 시각과 식별자 내림차순이다`() {
        // given
        val userId = user(110)
        register(userId, "desktop", DESKTOP)
        register(userId, "mobile", MOBILE)
        // when
        entityManager.createQuery("update UserFcmToken t set t.updatedAt = :now where t.userId = :userId")
            .setParameter("now", NOW.minusDays(1)).setParameter("userId", userId).executeUpdate()
        entityManager.clear()
        // then
        assertEquals("mobile", reader.readLatest(userId))
        register(userId, "desktop", DESKTOP)
        assertEquals("desktop", reader.readLatest(userId))
    }

    @Test
    fun `4096자 토큰은 저장소와 발송 대기열에 잘리지 않고 저장된다`() {
        // given
        val userId = user(111)
        val token = "a".repeat(4096)
        register(userId, token, DESKTOP)
        // when
        appender.append(NotificationAppendDto(
            deduplicationKey = "long-token", channel = NotificationChannel.FCM, templateCode = "fcm_test",
            recipientUserId = userId, recipientAddress = token, payloadJson = "{}", scheduledAt = NOW,
        ))
        tokens.flush()
        entityManager.clear()
        // then
        assertEquals(token, reader.readLatest(userId))
        assertEquals(token, notifications.findAllByDeduplicationKeyIn(listOf("long-token")).single().recipientAddress)
    }

    @Test
    fun `활성 토큰 해시는 DB에서 유일하다`() {
        // given
        val first = user(112)
        val second = user(113)
        register(first, "token", DESKTOP)
        // when, then
        assertThrows(DataIntegrityViolationException::class.java) {
            tokens.saveAndFlush(UserFcmToken(second, MOBILE.toString(), "token", MOBILE.toString()))
        }
    }

    @Test
    fun `활성 설치 슬롯은 계정이 달라도 DB에서 유일하다`() {
        // given
        val first = user(114)
        val second = user(115)
        register(first, "first", DESKTOP)
        // when, then
        assertThrows(DataIntegrityViolationException::class.java) {
            tokens.saveAndFlush(UserFcmToken(second, DESKTOP.toString(), "second", DESKTOP.toString()))
        }
    }

    @Test
    @Tag("concurrency")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `같은 계정과 설치의 동시 등록은 한 행으로 직렬화된다`() {
        // given
        val userId = user(201)
        val deviceId = UUID.fromString("c03a7396-907a-4e7f-a127-0cf5a80565ca")
        // when
        val results = concurrently(
            { register(userId, "concurrent-one", deviceId) },
            { register(userId, "concurrent-two", deviceId) },
        )
        // then
        assertTrue(results.all { it.isSuccess }, results.toString())
        assertEquals(1, reader.readActive(userId).size)
    }

    @Test
    @Tag("concurrency")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `다른 계정의 동시 등록에서도 같은 토큰의 활성 소유자는 한 명이다`() {
        // given
        val first = user(202)
        val second = user(203)
        val token = "concurrent-shared"
        // when
        val results = concurrently({ register(first, token) }, { register(second, token) })
        // then
        assertTrue(results.any { it.isSuccess })
        results.filter { it.isFailure }.forEach { assertInstanceOf(ConflictException::class.java, it.exceptionOrNull()) }
        assertEquals(1, (reader.readActive(first) + reader.readActive(second)).count { it.token == token })
    }

    @Test
    @Tag("concurrency")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `토큰 갱신과 이전 토큰 무효 응답이 경합해도 갱신 토큰은 남는다`() {
        // given
        val userId = user(204)
        transaction.executeWithoutResult { register(userId, "race-old") }
        // when
        val results = concurrently(
            { register(userId, "race-new") },
            { manager.revoke(userId, "race-old", NOW) },
        )
        // then
        assertTrue(results.all { it.isSuccess }, results.toString())
        assertEquals("race-new", reader.readLatest(userId))
    }

    private fun user(externalId: Long): Long = checkNotNull(users.saveAndFlush(User.ofLetsCareer(externalId, NOW)).id)

    private fun register(userId: Long, token: String, deviceId: UUID? = null) =
        manager.register(userId, UserFcmTokenRegisterDto(token, deviceId), NOW)

    private fun concurrently(vararg actions: () -> Unit): List<Result<Unit>> {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(actions.size)
        try {
            val tasks = actions.map { action -> executor.submit<Result<Unit>> {
                ready.countDown()
                check(start.await(5, TimeUnit.SECONDS))
                runCatching { transaction.executeWithoutResult { action() } }
            } }
            check(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            return tasks.map { it.get(15, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        private val NOW = LocalDateTime.of(2026, 10, 21, 20, 0)
        private val DESKTOP = UUID.fromString("a03a7396-907a-4e7f-a127-0cf5a80565ca")
        private val MOBILE = UUID.fromString("b03a7396-907a-4e7f-a127-0cf5a80565ca")
    }
}
