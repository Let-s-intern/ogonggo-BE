package com.ogonggo.userapi.usernotification

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.NotificationBatchAppender
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchAppendDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import com.ogonggo.userapi.usernotification.business.UserNotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.support.TransactionTemplate
import java.time.*
import java.util.UUID

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:broadcast-inbox;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
    "ogonggo.letscareer.base-url=http://localhost:8090", "ogonggo.letscareer.internal-api-key=test-key", "spring.mail.host=localhost",
])
@AutoConfigureMockMvc
@Import(NotificationBroadcastIntegrationTest.FixedTime::class)
class NotificationBroadcastIntegrationTest @Autowired constructor(
    private val users: UserAppender, private val batches: NotificationBatchAppender,
    private val personal: UserNotificationAppender, private val inbox: UserNotificationService,
    private val tx: TransactionTemplate, private val jdbc: JdbcTemplate, private val mvc: MockMvc,
) {
    @MockBean private lateinit var dispatcher: NotificationDispatcher

    @BeforeEach
    fun clean() {
        listOf("notification_broadcast_reads", "user_notifications", "notification_batches").forEach { jdbc.update("delete from $it") }
    }

    @Test
    @DisplayName("토큰이 없는 회원도 개인 알림과 전체 공지를 같은 시각에 빠짐없이 페이지 조회한다")
    fun `개인 알림과 공통 공지를 합쳐 조회한다`() {
        // given
        val user = user()
        val id = personal.append(UserNotificationAppendDto(user, UserNotificationType.ADMIN_NOTIFICATION, 42,
            null, null, "오공고", null, "개인", "미리보기", null, batchId = UUID.randomUUID().toString(), url = "https://example.com/personal"))!!
        broadcast(1); broadcast(2)
        // when
        val first = inbox.list(user, null, 1)
        broadcast(3)
        val second = inbox.list(user, first.nextCursor, 1)
        val third = inbox.list(user, second.nextCursor, 1)
        // then
        assertEquals(uuid(2).toString(), first.items.single().broadcastId)
        assertEquals(uuid(1).toString(), second.items.single().broadcastId)
        assertEquals(id, third.items.single().id)
        assertFalse(third.hasNext)
        assertEquals(4, inbox.countUnread(user))
        mvc.perform(get(PATH).with(auth(user))).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].source").value("BROADCAST"))
            .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].batchId").value(uuid(3).toString()))
            .andExpect(jsonPath("$.data.items[0].body").value("전체 본문"))
        assertEquals(1, jdbc.queryForObject("select count(*) from user_notifications", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_broadcast_reads", Int::class.java))
    }

    @Test
    @DisplayName("공통 공지 읽음은 회원별로 격리되고 반복 호출에도 최초 시각만 남긴다")
    fun `공통 공지를 회원별로 읽는다`() {
        // given
        val first = user(); val second = user()
        broadcast(1); broadcast(2)
        // when
        repeat(2) { mvc.perform(patch("$PATH/broadcasts/${uuid(1)}/read").with(auth(first))).andExpect(status().isOk) }
        val readAt = jdbc.queryForObject("select read_at from notification_broadcast_reads", LocalDateTime::class.java)
        inbox.markAllRead(first)
        // then
        assertEquals(NOW, readAt)
        assertEquals(0, inbox.countUnread(first))
        assertEquals(2, inbox.countUnread(second))
        assertEquals(2, jdbc.queryForObject("select count(*) from notification_broadcast_reads", Int::class.java))
        mvc.perform(patch("$PATH/broadcasts/${uuid(1)}/read").with(auth(first))).andExpect(status().isOk)
        assertEquals(readAt, jdbc.queryForObject("select read_at from notification_broadcast_reads where batch_id = ?", LocalDateTime::class.java, uuid(1).toString()))
    }

    @Test
    @DisplayName("가입 전·기간 만료·미래 공지와 과거 개별 발송 배치는 공통 알림함에 포함하지 않는다")
    fun `공통 공지 노출 조건을 읽음과 목록에 같이 적용한다`() {
        // given
        val user = user(NOW.minusDays(10))
        broadcast(1); broadcast(2); broadcast(3); broadcast(4, topic = null)
        jdbc.update("update notification_batches set created_at = ? where id = ?", NOW.minusDays(11), uuid(1).toString())
        jdbc.update("update notification_batches set created_at = ? where id = ?", NOW.minusDays(31), uuid(2).toString())
        jdbc.update("update notification_batches set created_at = ? where id = ?", NOW.plusSeconds(1), uuid(3).toString())
        // when
        inbox.markAllRead(user)
        // then
        assertTrue(inbox.list(user, null, 20).items.isEmpty())
        assertEquals(0, inbox.countUnread(user))
        (1..4).forEach { mvc.perform(patch("$PATH/broadcasts/${uuid(it)}/read").with(auth(user)))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOTIFICATION_NOT_FOUND")) }
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_broadcast_reads", Int::class.java))
    }

    @Test
    @DisplayName("공통 공지 읽음도 인증·활성 회원과 올바른 UUID를 요구한다")
    fun `공통 공지 읽음 API를 보호한다`() {
        // given
        val user = user()
        broadcast(1)
        // when
        mvc.perform(patch("$PATH/broadcasts/${uuid(1)}/read")).andExpect(status().isUnauthorized)
        mvc.perform(patch("$PATH/broadcasts/not-a-uuid/read").with(auth(user))).andExpect(status().isBadRequest)
        jdbc.update("update users set status = 'SUSPENDED' where id = ?", user)
        // then
        mvc.perform(patch("$PATH/broadcasts/${uuid(1)}/read").with(auth(user))).andExpect(status().isForbidden)
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_broadcast_reads", Int::class.java))
    }

    private fun broadcast(number: Int, topic: String? = "ogonggo-all-test") = batches.append(NotificationBatchAppendDto(
        batchId = uuid(number).toString(), requestedByUserId = 42, targetType = NotificationBatchTargetType.ALL,
        requestedUserIdsJson = null, title = "전체 공지", body = "전체 본문", dataJson = "{}",
        targetUserCount = 2, recipientUserCount = 1, enqueuedCount = 1, skippedNoTokenUserCount = 1,
        skippedInactiveUserCount = 0, dispatchExpiresAt = NOW.plusHours(24), topicName = topic, url = "https://example.com/notice",
    ))
    private fun user(joinedAt: LocalDateTime = NOW.minusDays(1)) = tx.execute { users.append(UserAppendDto(IDS.incrementAndGet(), joinedAt)).userId }!!
    private fun auth(id: Long) = authentication(UsernamePasswordAuthenticationToken(id, null, emptyList()))
    private fun uuid(number: Int) = UUID(0, number.toLong())

    @TestConfiguration
    class FixedTime { @Bean @Primary fun broadcastTestClock(): Clock = CLOCK }
    companion object {
        private const val PATH = "/api/v1/users/me/notifications"
        private val CLOCK = Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        private val NOW = LocalDateTime.now(CLOCK)
        private val IDS = java.util.concurrent.atomic.AtomicLong(710000)
    }
}
