package com.ogonggo.adminapi.notificationbatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.UserAppendDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-notification-topics;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ogonggo.notification.broadcast.enabled=true", "ogonggo.notification.broadcast.topic=ogonggo-all-test",
])
@AutoConfigureMockMvc
class AdminNotificationTopicIntegrationTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val users: UserAppender,
    private val jdbc: JdbcTemplate, private val tx: TransactionTemplate, private val clock: Clock,
    private val tokens: UserFcmTokenManager, private val notifications: NotificationManager,
) {
    @BeforeEach
    fun clean() {
        listOf("notification_broadcast_reads", "fcm_topic_sync_tasks", "notifications", "user_notifications",
            "notification_batches", "user_fcm_tokens", "users").forEach { jdbc.update("delete from $it") }
    }

    @Test
    @DisplayName("전체 공지는 회원 수와 관계없이 토픽 작업 한 건을 적재하고 회원별 알림함은 생성하지 않는다")
    fun `전체 공지는 토픽 작업 한 건으로 적재한다`() {
        // given
        tx.executeWithoutResult { repeat(3) { users.append(UserAppendDto(990000L + it, LocalDateTime.now(clock))) } }
        val ids = jdbc.queryForList("select id from users order by id", Long::class.java)
        tx.executeWithoutResult {
            tokens.register(ids[0], UserFcmTokenRegisterDto("desktop", UUID.randomUUID()), LocalDateTime.now(clock))
            tokens.register(ids[0], UserFcmTokenRegisterDto("mobile", UUID.randomUUID()), LocalDateTime.now(clock))
            tokens.register(ids[1], UserFcmTokenRegisterDto("another", UUID.randomUUID()), LocalDateTime.now(clock))
        }
        val request = mapOf("targetType" to "ALL", "title" to "공지", "body" to "한".repeat(1000),
            "data" to mapOf("url" to "https://example.com/notice", "notificationId" to "999", "batchId" to "fake", "source" to "PERSONAL"))
        val auth = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

        // when
        val response = mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated).andReturn()
        val result = mapper.readTree(response.response.contentAsByteArray)["data"]

        // then
        assertEquals(1, result["enqueuedCount"].asInt())
        assertEquals("TOPIC", result["deliveryTargetType"].asText())
        assertEquals(1, result["noTokenUserCount"].asInt())
        assertEquals(2, result["recipientUserCount"].asInt())
        assertEquals(1, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from user_notifications", Int::class.java))
        assertEquals("ogonggo-all-test", jdbc.queryForObject("select recipient_address from notifications", String::class.java))
        assertEquals("한".repeat(1000), jdbc.queryForObject("select body from notification_batches", String::class.java))
        val payload = mapper.readTree(jdbc.queryForObject("select payload_json from notifications", String::class.java))
        assertEquals("한".repeat(100), payload["body"].asText())
        assertEquals(result["batchId"].asText(), payload.at("/data/batchId").asText())
        assertEquals("BROADCAST", payload.at("/data/source").asText())
        assertFalse(payload["data"].has("notificationId"))
        val notificationId = jdbc.queryForObject("select id from notifications", Long::class.java)!!
        val claimed = notifications.claimDue(LocalDateTime.now(clock), 10).single()
        notifications.complete(
            notificationId,
            checkNotNull(claimed.processingAttemptId),
            NotificationDeliveryResult.Sent("topic-message"),
            LocalDateTime.now(clock),
        )
        val read = mvc.perform(get("/api/v1/admin/notification-batches/${result["batchId"].asText()}").with(authentication(auth)))
            .andExpect(status().isOk).andReturn()
        val delivery = mapper.readTree(read.response.contentAsByteArray)["data"]
        assertEquals("TOPIC", delivery["deliveryTargetType"].asText())
        assertEquals(1, delivery["sentCount"].asInt())
        assertTrue(delivery["completed"].asBoolean())
    }

    @Test
    @DisplayName("토픽 payload가 2048바이트를 넘으면 공지와 작업을 저장하지 않는다")
    fun `큰 data는 적재 전에 거부한다`() {
        // given
        val request = mapOf("targetType" to "ALL", "title" to "공지", "body" to "본문",
            "data" to mapOf("url" to "https://example.com/notice", "large" to "가".repeat(1000)))
        val auth = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
        // when
        mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request))).andExpect(status().isBadRequest)
        // then
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
    }

    @Test
    @DisplayName("토픽 작업 저장 실패는 이미 저장한 공통 공지까지 롤백한다")
    fun `공지와 토픽 작업을 함께 롤백한다`() {
        // given
        val request = mapOf("targetType" to "ALL", "title" to "공지", "body" to "본문", "data" to mapOf("url" to "https://example.com/notice"))
        val auth = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
        jdbc.execute("alter table notifications add constraint reject_topic_fixture check (recipient_type <> 'TOPIC')")
        try {
            // when
            mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request))).andExpect(status().isInternalServerError)
            // then
            assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
            assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
        } finally { jdbc.execute("alter table notifications drop constraint reject_topic_fixture") }
    }
}
