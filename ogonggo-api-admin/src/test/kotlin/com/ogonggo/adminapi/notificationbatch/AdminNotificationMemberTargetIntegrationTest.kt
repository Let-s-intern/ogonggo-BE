package com.ogonggo.adminapi.notificationbatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
import java.util.UUID

/** 회원 종류 토픽 발송이 켜진 환경에서 일반회원(GENERAL_MEMBERS)·기업회원(COMPANY_MEMBERS) 대상 발송이 토픽 작업 한 건만 적재하는지 확인한다. */
@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-member-topics;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ogonggo.notification.broadcast.topic=ogonggo-all-test",
    "ogonggo.notification.broadcast.member-topics-enabled=true",
    "ogonggo.notification.broadcast.general-topic=ogonggo-general-test",
    "ogonggo.notification.broadcast.company-topic=ogonggo-company-test",
])
@AutoConfigureMockMvc
class AdminNotificationMemberTopicIntegrationTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val users: UserAppender,
    private val jdbc: JdbcTemplate, private val tx: TransactionTemplate, private val clock: Clock,
    private val tokens: UserFcmTokenManager,
) {
    @BeforeEach
    fun setUp() {
        MemberTargetFixture.clean(jdbc)
        MemberTargetFixture.seedMembers(users, tokens, tx, clock)
    }

    @Test
    @DisplayName("기업 회원 대상 발송은 기업회원 토픽 작업 한 건을 적재하고 대상 수는 활성 기업 회원만 센다")
    fun `기업 회원 대상은 기업회원 토픽으로 보낸다`() {
        // when
        val result = MemberTargetFixture.send(mvc, mapper, "COMPANY_MEMBERS")

        // then
        assertEquals("TOPIC", result["deliveryTargetType"].asText())
        assertEquals(1, result["enqueuedCount"].asInt())
        assertEquals(2, result["targetUserCount"].asInt())
        assertEquals(1, result["noTokenUserCount"].asInt())
        assertEquals("ogonggo-company-test", jdbc.queryForObject("select recipient_address from notifications", String::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from user_notifications", Int::class.java))
    }

    @Test
    @DisplayName("일반 회원 대상 발송은 일반회원 토픽으로 보내고 대상 수에 기업 회원을 넣지 않는다")
    fun `일반 회원 대상은 일반회원 토픽으로 보낸다`() {
        // when
        val result = MemberTargetFixture.send(mvc, mapper, "GENERAL_MEMBERS")

        // then
        assertEquals("TOPIC", result["deliveryTargetType"].asText())
        assertEquals(3, result["targetUserCount"].asInt())
        assertEquals("ogonggo-general-test", jdbc.queryForObject("select recipient_address from notifications", String::class.java))
    }
}

/** 회원 종류 토픽 발송이 꺼진 환경에서 대상 회원마다 알림함을, 기기마다 발송 행을 만드는지 확인한다. */
@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-member-direct;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@AutoConfigureMockMvc
class AdminNotificationMemberDirectIntegrationTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val users: UserAppender,
    private val jdbc: JdbcTemplate, private val tx: TransactionTemplate, private val clock: Clock,
    private val tokens: UserFcmTokenManager,
) {
    @BeforeEach
    fun setUp() {
        MemberTargetFixture.clean(jdbc)
        MemberTargetFixture.seedMembers(users, tokens, tx, clock)
    }

    @Test
    @DisplayName("토픽 발송이 꺼져 있으면 일반 회원 대상 발송은 일반 회원에게만 알림함과 기기별 발송을 만든다")
    fun `일반 회원 대상은 일반 회원 기기마다 보낸다`() {
        // when
        val result = MemberTargetFixture.send(mvc, mapper, "GENERAL_MEMBERS")

        // then
        assertEquals("DIRECT", result["deliveryTargetType"].asText())
        assertEquals(3, result["targetUserCount"].asInt())
        assertEquals(2, result["enqueuedCount"].asInt())
        val recipientRoles = jdbc.queryForList(
            "select u.role from user_notifications n join users u on u.id = n.recipient_user_id", String::class.java)
        assertEquals(listOf("USER", "USER", "USER"), recipientRoles)
    }

    @Test
    @DisplayName("기업 회원 대상 발송에 userIds를 보내면 400으로 거부한다")
    fun `회원 종류 대상에는 userIds를 받지 않는다`() {
        // given
        val request = mapOf("targetType" to "COMPANY_MEMBERS", "userIds" to listOf(1), "title" to "공지", "body" to "본문",
            "data" to mapOf("url" to "https://example.com/notice"))

        // when / then
        mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(MemberTargetFixture.admin()))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isBadRequest)
    }
}

private object MemberTargetFixture {
    fun admin() = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    fun clean(jdbc: JdbcTemplate) {
        listOf("notification_broadcast_reads", "fcm_topic_sync_tasks", "notifications", "user_notifications",
            "notification_batches", "user_fcm_tokens", "users").forEach { jdbc.update("delete from $it") }
    }

    /** 일반 회원 3명(그중 토큰 있는 회원 2명), 기업 회원 2명(그중 토큰 있는 회원 1명)을 만든다. */
    fun seedMembers(users: UserAppender, tokens: UserFcmTokenManager, tx: TransactionTemplate, clock: Clock) {
        val now = LocalDateTime.now(clock)
        val general = tx.execute { (0 until 3).map { users.append(UserAppendDto(980000L + it, now)).userId } }!!
        val company = tx.execute {
            (0 until 2).map { users.appendCompany(CompanyAccountAppendDto("company$it@example.com", "encoded", now)).userId }
        }!!
        tx.executeWithoutResult {
            tokens.register(general[0], UserFcmTokenRegisterDto("general-0", UUID.randomUUID()), now)
            tokens.register(general[1], UserFcmTokenRegisterDto("general-1", UUID.randomUUID()), now)
            tokens.register(company[0], UserFcmTokenRegisterDto("company-0", UUID.randomUUID()), now)
        }
    }

    fun send(mvc: MockMvc, mapper: ObjectMapper, targetType: String): JsonNode {
        val request = mapOf("targetType" to targetType, "title" to "공지", "body" to "본문",
            "data" to mapOf("url" to "https://example.com/notice"))
        val response = mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(admin()))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated).andReturn()
        return mapper.readTree(response.response.contentAsByteArray)["data"]
    }
}
