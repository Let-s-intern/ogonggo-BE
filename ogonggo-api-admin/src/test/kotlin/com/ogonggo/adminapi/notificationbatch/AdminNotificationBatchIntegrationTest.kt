package com.ogonggo.adminapi.notificationbatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-notification-batches;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@AutoConfigureMockMvc
@Import(AdminNotificationBatchIntegrationTest.FixedTime::class)
class AdminNotificationBatchIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val users: UserAppender,
    private val tokens: UserFcmTokenManager,
    private val notifications: NotificationManager,
    private val jdbc: JdbcTemplate,
    private val tx: TransactionTemplate,
    private val clock: TestClock,
) {
    @BeforeEach
    fun reset() {
        clock.current = START
        listOf("notifications", "notification_batches", "user_fcm_tokens", "users").forEach { jdbc.update("delete from $it") }
    }

    @Test
    @DisplayName("지정 회원의 활성 설치마다 적재하고 요청자·본문·대상·제외 사유를 기록한다")
    fun `다중 기기 발송 건수와 회원 제외 건수를 구분한다`() {
        // given
        val receiver = user()
        val noToken = user()
        val suspended = user()
        register(receiver, "desktop")
        register(receiver, "mobile")
        register(suspended, "suspended-token")
        jdbc.update("update users set status = 'SUSPENDED' where id = ?", suspended)

        // when
        val batch = create(request(listOf(receiver, receiver, noToken, suspended)))
        val id = batch["batchId"].asText()

        // then
        assertEquals(3, batch["targetUserCount"].asInt())
        assertEquals(1, batch["recipientUserCount"].asInt())
        assertEquals(2, batch["enqueuedCount"].asInt())
        assertEquals(1, batch["skippedNoTokenUserCount"].asInt())
        assertEquals(1, batch["skippedInactiveUserCount"].asInt())
        val record = jdbc.queryForMap("select * from notification_batches where id = ?", id)
        assertEquals(ADMIN_ID, (record["requested_by_user_id"] as Number).toLong())
        assertEquals("제목", record["title"])
        assertEquals("본문", record["body"])
        assertEquals(listOf(receiver, noToken, suspended).sorted(), mapper.readTree(record["requested_user_ids_json"].toString()).map { it.asLong() })
        assertEquals(URL, mapper.readTree(record["data_json"].toString())["url"].asText())
        val rows = jdbc.queryForList("select * from notifications where batch_id = ?", id)
        assertEquals(setOf("desktop", "mobile"), rows.map { it["recipient_address"] }.toSet())
        assertTrue(rows.all { it["user_notification_id"] == null })
        assertTrue(rows.all { mapper.readTree(it["payload_json"].toString()).at("/data/url").asText() == URL })
        val result = result(id)
        assertEquals(2, result["pendingCount"].asInt())
        assertFalse(result["completed"].asBoolean())
        assertEquals("2026-10-23T12:00:00", result["dispatchExpiresAt"].asText())
        assertEquals("2026-10-22T12:00:00", result["createdAt"].asText())
    }

    @Test
    @DisplayName("전체 발송은 회원 페이지를 넘겨 활성 회원만 평가하고 토큰 없는 회원 수를 센다")
    fun `전체 발송은 500명 이후 회원도 빠뜨리지 않는다`() {
        // given
        val ids = tx.execute { (1..502).map { users.append(UserAppendDto(IDS.incrementAndGet(), NOW)).userId } }!!
        val inactive = user()
        register(ids.first(), "first")
        register(ids.last(), "last")
        register(inactive, "inactive")
        jdbc.update("update users set status = 'WITHDRAWN' where id = ?", inactive)

        // when
        val batch = create(request(null, "ALL"))

        // then
        assertEquals(502, batch["targetUserCount"].asInt())
        assertEquals(2, batch["recipientUserCount"].asInt())
        assertEquals(2, batch["enqueuedCount"].asInt())
        assertEquals(500, batch["skippedNoTokenUserCount"].asInt())
        assertEquals(0, batch["skippedInactiveUserCount"].asInt())
        assertEquals(setOf(ids.first(), ids.last()), jdbc.queryForList("select recipient_user_id from notifications", Long::class.java).toSet())
    }

    @Test
    @DisplayName("대상이 없거나 모두 토큰이 없어도 조회 가능한 운영 기록을 남긴다")
    fun `영 건 발송도 묶음을 조회할 수 있다`() {
        // given
        val user = user()

        // when
        val batch = create(request(listOf(user)))
        val result = result(batch["batchId"].asText())

        // then
        assertEquals(0, result["enqueuedCount"].asInt())
        assertEquals(1, result["skippedNoTokenUserCount"].asInt())
        assertTrue(result["completed"].asBoolean())
        assertEquals(0, result["pendingCount"].asInt())
        jdbc.update("delete from users")
        val empty = create(request(null, "ALL"))
        assertEquals(0, empty["targetUserCount"].asInt())
        assertTrue(result(empty["batchId"].asText())["completed"].asBoolean())
    }

    @Test
    @DisplayName("묶음별 성공·실패·미확정·대기를 분리하고 resultCode별 사유를 반환한다")
    fun `발송 결과는 다른 묶음과 섞이지 않는다`() {
        // given
        val user = user()
        repeat(4) { register(user, "token-$it") }
        val batch = create(request(listOf(user)))["batchId"].asText()
        create(request(listOf(user)))
        val ids = jdbc.queryForList("select id from notifications where batch_id = ? order by id", Long::class.java, batch)
        notifications.complete(ids[0], NotificationDeliveryResult.Sent("provider"), NOW)
        notifications.complete(ids[1], NotificationDeliveryResult.Failed("UNREGISTERED", NotificationFailureCategory.CLIENT_ERROR), NOW)
        notifications.complete(ids[2], NotificationDeliveryResult.Unknown("TRANSPORT_ERROR", NotificationFailureCategory.TRANSPORT_ERROR), NOW)

        // when
        val result = result(batch)

        // then
        listOf("sentCount", "failedCount", "unknownCount", "pendingCount").forEach { assertEquals(1, result[it].asInt(), it) }
        assertFalse(result["completed"].asBoolean())
        assertEquals(setOf("UNREGISTERED", "TRANSPORT_ERROR"), result["failureReasons"].map { it["resultCode"].asText() }.toSet())
        assertTrue(result["failureReasons"].all { it["count"].asInt() == 1 })
        assertFalse(result.has("recipientAddress"))
        notifications.complete(ids[3], NotificationDeliveryResult.Sent("provider-2"), NOW)
        assertTrue(result(batch)["completed"].asBoolean())
    }

    @Test
    @DisplayName("24시간 경계에서 미처리를 기한 초과로 표시하고 30일 경계부터 결과 조회를 닫는다")
    fun `대기 만료와 결과 보관 기간을 구분한다`() {
        // given
        val user = user()
        register(user, "test")
        val batch = create(request(listOf(user)))["batchId"].asText()

        // when
        clock.current = START.plusSeconds(24 * 3600)
        val expired = result(batch)

        // then
        assertEquals(0, expired["pendingCount"].asInt())
        assertEquals(1, expired["expiredCount"].asInt())
        assertEquals(0, expired["failedCount"].asInt())
        assertTrue(expired["completed"].asBoolean())
        assertTrue(notifications.findDue(NOW.plusDays(1), NOW.plusDays(1).minusMinutes(8), 10).isEmpty())
        clock.current = START.plusSeconds(30L * 24 * 3600)
        mvc.perform(admin(get("$PATH/$batch"))).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOTIFICATION_BATCH_NOT_FOUND"))
        assertEquals(1, jdbc.queryForObject("select count(*) from notification_batches where id = ?", Int::class.java, batch))
    }

    @Test
    @DisplayName("없는 지정 회원이나 운영 기록 저장 실패가 있으면 대기열까지 전체 롤백한다")
    fun `불완전한 발송은 적재하지 않는다`() {
        // given
        val user = user()
        register(user, "test")

        // when, then
        mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request(listOf(user, Long.MAX_VALUE)))))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
        jdbc.execute("alter table notification_batches add constraint reject_test_title check (title <> 'rollback-fixture')")
        try {
            mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(request(listOf(user)) + ("title" to "rollback-fixture"))))
                .andExpect(status().isInternalServerError)
            assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
            assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
        } finally {
            jdbc.execute("alter table notification_batches drop constraint reject_test_title")
        }
    }

    @Test
    @DisplayName("관리자만 발송을 요청하고 결과를 조회할 수 있다")
    fun `두 API는 관리자 인증을 요구한다`() {
        // given
        val json = mapper.writeValueAsBytes(request(listOf(1)))
        val id = UUID.randomUUID()

        // when, then
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isUnauthorized)
        mvc.perform(get("$PATH/$id")).andExpect(status().isUnauthorized)
        val member = UsernamePasswordAuthenticationToken(1L, null, emptyList())
        mvc.perform(post(PATH).with(authentication(member)).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden)
        mvc.perform(get("$PATH/$id").with(authentication(member))).andExpect(status().isForbidden)
        mvc.perform(admin(get("$PATH/$id"))).andExpect(status().isNotFound)
        mvc.perform(admin(get("$PATH/invalid-uuid"))).andExpect(status().isBadRequest)
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
    }

    @Test
    @DisplayName("대상 조합·본문 길이·문자열 맵·이동 URL·FCM payload 크기를 검증한다")
    fun `잘못된 입력을 저장 전에 거부한다`() {
        // given
        val base = request(listOf(1))
        val invalid = listOf(
            base + ("title" to "x".repeat(101)), base + ("title" to " "), base + ("body" to "x".repeat(1001)),
            base + ("userIds" to emptyList<Long>()), base + ("userIds" to listOf(-1)), base + ("userIds" to listOf(null)),
            base + ("targetType" to "ALL"), base + ("data" to emptyMap<String, String>()),
            base + ("data" to mapOf("url" to "javascript:alert(1)")),
            base + ("data" to mapOf("url" to URL, "value" to 7)),
            base + ("data" to mapOf("url" to URL, "value" to null)),
            base + ("data" to mapOf("url" to URL, "google.bad" to "x")),
            base + ("data" to mapOf("url" to URL, "content" to "한".repeat(1400))),
        )

        // when, then
        invalid.forEach { request ->
            mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
    }

    private fun request(userIds: List<Long>?, type: String = "USER_IDS"): Map<String, Any> = buildMap {
        put("targetType", type)
        if (userIds != null) put("userIds", userIds)
        put("title", "제목"); put("body", "본문"); put("data", mapOf("url" to URL))
    }

    private fun create(request: Map<String, Any?>): JsonNode = mapper.readTree(mvc.perform(admin(post(PATH))
        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated).andReturn().response.contentAsString)["data"]

    private fun result(batchId: String): JsonNode = mapper.readTree(mvc.perform(admin(get("$PATH/$batchId")))
        .andExpect(status().isOk).andReturn().response.contentAsString)["data"]

    private fun user(): Long = tx.execute { users.append(UserAppendDto(IDS.incrementAndGet(), NOW)).userId }!!
    private fun register(userId: Long, token: String) = tx.executeWithoutResult { tokens.register(userId, UserFcmTokenRegisterDto(token, UUID.randomUUID()), NOW) }
    private fun admin(request: MockHttpServletRequestBuilder) = request.with(authentication(
        UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))))

    class TestClock : Clock() {
        var current: Instant = START
        override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
        override fun withZone(zone: ZoneId): Clock = fixed(current, zone)
        override fun instant(): Instant = current
    }
    @TestConfiguration
    class FixedTime { @Bean @Primary fun batchClock() = TestClock() }

    private companion object {
        const val PATH = "/api/v1/admin/notification-batches"
        const val ADMIN_ID = 42L
        const val URL = "https://www.ogonggo.co.kr/announcements/10"
        val START: Instant = Instant.parse("2026-10-22T03:00:00Z")
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 22, 12, 0)
        val IDS = java.util.concurrent.atomic.AtomicLong(1_000_000)
    }
}
