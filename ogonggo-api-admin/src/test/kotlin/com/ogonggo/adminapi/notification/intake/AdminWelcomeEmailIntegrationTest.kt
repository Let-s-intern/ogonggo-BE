package com.ogonggo.adminapi.notification.intake

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.user.implement.dto.UserProfileSyncDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-welcome-email;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@AutoConfigureMockMvc
@Import(AdminWelcomeEmailIntegrationTest.FixedTime::class)
class AdminWelcomeEmailIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val users: UserAppender,
    private val profiles: UserProfileManager,
    private val notifications: NotificationAppender,
    private val jdbc: JdbcTemplate,
    private val tx: TransactionTemplate,
) {

    @BeforeEach
    fun reset() {
        listOf("notifications", "user_profiles", "users").forEach { jdbc.update("delete from $it") }
    }

    @Test
    @DisplayName("재발송마다 새 기록을 만들고 기존 성공 이력과 원래 수신 주소는 보존한다")
    fun `현재 가입 이메일로 환영 메일을 다시 적재한다`() {
        // given
        val userId = user()
        val originalId = notifications.appendAndGetId(NotificationAppendDto(
            deduplicationKey = "sign_up_welcome:user:$userId:EMAIL",
            channel = NotificationChannel.EMAIL,
            templateCode = "sign_up_welcome",
            recipientAddress = "previous@example.com",
            payloadJson = "{}",
            scheduledAt = NOW.minusDays(1),
            recipientUserId = userId,
        ))!!
        jdbc.update("update notifications set status = 'SENT', sent_at = ? where id = ?", NOW.minusDays(1), originalId)
        val original = jdbc.queryForMap("select * from notifications where id = ?", originalId)

        // when
        val first = resend(userId)
        val second = resend(userId)

        // then
        assertNotEquals(first["notificationId"].asLong(), second["notificationId"].asLong())
        assertEquals(original, jdbc.queryForMap("select * from notifications where id = ?", originalId))
        listOf(first, second).forEach { response ->
            assertEquals(userId, response["recipientUserId"].asLong())
            assertEquals("sign_up_welcome", response["templateCode"].asText())
            assertEquals(NOW.toString(), response["scheduledAt"].asText())
            assertFalse(response.has("recipientAddress"))
            val saved = jdbc.queryForMap("select * from notifications where id = ?", response["notificationId"].asLong())
            assertEquals("EMAIL", saved["channel"])
            assertEquals("PENDING", saved["status"])
            assertEquals("member@example.com", saved["recipient_address"])
            assertEquals("{}", saved["payload_json"])
            assertEquals(Timestamp.valueOf(NOW), saved["scheduled_at"])
            assertEquals(Timestamp.valueOf(NOW.plusHours(24)), saved["dispatch_expires_at"])
            assertTrue(saved["deduplication_key"].toString().contains(":admin:$ADMIN_ID:resend:"))
            assertNull(saved["batch_id"])
            assertNull(saved["sent_at"])
        }
        val recent = mapper.readTree(mvc.perform(admin(get(RECENT_PATH)))
            .andExpect(status().isOk).andReturn().response.contentAsByteArray).at("/data/items")
        assertTrue(recent.any { it["notificationId"].asLong() == second["notificationId"].asLong() })
    }

    @Test
    @DisplayName("기존 발송 기록이 없어도 활성 일반 회원의 USER·MENTOR·ADMIN 역할 모두 재발송할 수 있다")
    fun `이력 유실 회원도 역할과 무관하게 재발송한다`() {
        // given
        val userIds = listOf("USER", "MENTOR", "ADMIN").map { role ->
            user().also { jdbc.update("update users set role = ? where id = ?", role, it) }
        }

        // when
        userIds.forEach(::resend)

        // then
        assertEquals(userIds.toSet(), jdbc.queryForList("select recipient_user_id from notifications", Long::class.java).toSet())
    }

    @Test
    @DisplayName("기업 회원과 정지·탈퇴 회원은 이메일을 적재하지 않고 403으로 거부한다")
    fun `대상이 아닌 회원에게 발송하지 않는다`() {
        // given
        val company = tx.execute { users.appendCompany(CompanyAccountAppendDto("company@example.com", "encoded", NOW)).userId }!!
        val suspended = user().also { jdbc.update("update users set status = 'SUSPENDED' where id = ?", it) }
        val withdrawn = user().also { jdbc.update("update users set status = 'WITHDRAWN' where id = ?", it) }

        // when, then
        mapOf(company to "GENERAL_MEMBER_REQUIRED", suspended to "USER_SUSPENDED", withdrawn to "USER_WITHDRAWN").forEach { (id, code) ->
            mvc.perform(admin(post(path(id))))
                .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value(code))
        }
        assertNoNotifications()
    }

    @Test
    @DisplayName("회원이 없으면 404이며 프로필·가입 이메일이 없으면 409이고 발송 기록을 만들지 않는다")
    fun `수신자를 확인할 수 없으면 적재하지 않는다`() {
        // given
        val noProfile = user().also { jdbc.update("delete from user_profiles where user_id = ?", it) }
        val noEmail = user(email = null)
        val blankEmail = user(email = " ")

        // when, then
        mvc.perform(admin(post(path(Long.MAX_VALUE))))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        listOf(noProfile, noEmail, blankEmail).forEach { id ->
            mvc.perform(admin(post(path(id))))
                .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("WELCOME_EMAIL_RECIPIENT_UNAVAILABLE"))
        }
        assertNoNotifications()
    }

    @Test
    @DisplayName("관리자 인증과 양수 회원 ID를 검증하고 거부한 요청은 적재하지 않는다")
    fun `권한과 경로 입력을 검증한다`() {
        // given
        val userId = user()
        val member = UsernamePasswordAuthenticationToken(userId, null, emptyList())

        // when, then
        mvc.perform(post(path(userId))).andExpect(status().isUnauthorized)
        mvc.perform(post(path(userId)).with(authentication(member))).andExpect(status().isForbidden)
        listOf(0L, -1L).forEach { id ->
            mvc.perform(admin(post(path(id))))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        assertNoNotifications()
    }

    private fun user(email: String? = "member@example.com"): Long = tx.execute {
        val id = users.append(UserAppendDto(IDS.incrementAndGet(), NOW.minusDays(1))).userId
        profiles.sync(UserProfileSyncDto(
            userId = id, name = null, email = email, phoneNum = null, letsCareerAuthProvider = null,
            nickname = null, profileImageUrl = null, letsCareerUpdatedAt = null, syncedAt = NOW,
        ))
        id
    }!!

    private fun resend(userId: Long): JsonNode = mapper.readTree(
        mvc.perform(admin(post(path(userId))))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andReturn().response.contentAsByteArray,
    )["data"]

    private fun assertNoNotifications() {
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
    }

    private fun path(userId: Long) = "/api/v1/admin/general-members/$userId/welcome-email-resends"

    private fun admin(request: MockHttpServletRequestBuilder) = request.with(authentication(
        UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))),
    ))

    @TestConfiguration
    class FixedTime {
        @Bean
        @Primary
        fun welcomeEmailClock(): Clock = Clock.fixed(Instant.parse("2026-10-26T03:34:56Z"), ZoneId.of("Asia/Seoul"))
    }

    private companion object {
        const val ADMIN_ID = 42L
        const val RECENT_PATH = "/api/v1/admin/notifications/recent-results"
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 26, 12, 34, 56)
        val IDS = AtomicLong(2_000_000)
    }
}
