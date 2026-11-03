package com.ogonggo.adminapi.notificationbatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.notificationbatch.error.AdminNotificationBatchErrorCode
import com.ogonggo.adminapi.notificationbatch.implement.LetsCareerMarketingAgreementClient
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 희망 직무(JOB_ROLES) 발송과 광고성 발송을 확인한다. 렛츠커리어 마케팅 수신 동의 조회는 대역으로 바꾼다.
 * 회원: 일반 회원 4명(직무 백엔드·마케터·개발 직무 전체·직무 무관, 렛츠커리어 ID 970001~970004, 모두 토큰 있음), 기업 회원 1명.
 */
@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-job-roles-ads;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    // 광고성 발송이 토픽을 쓰지 않는지 보려고 토픽 게이트를 모두 켠다.
    "ogonggo.notification.broadcast.enabled=true", "ogonggo.notification.broadcast.topic=ogonggo-all-test",
    "ogonggo.notification.broadcast.member-topics-enabled=true",
    "ogonggo.notification.broadcast.general-topic=ogonggo-general-test",
    "ogonggo.notification.broadcast.company-topic=ogonggo-company-test",
])
@AutoConfigureMockMvc
class AdminNotificationJobRoleAndAdvertisementIntegrationTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val users: UserAppender,
    private val jdbc: JdbcTemplate, private val tx: TransactionTemplate, private val clock: Clock,
    private val tokens: UserFcmTokenManager,
) {
    @MockBean private lateinit var marketingAgreements: LetsCareerMarketingAgreementClient

    @BeforeEach
    fun setUp() {
        listOf("notification_broadcast_reads", "fcm_topic_sync_tasks", "notifications", "user_notifications",
            "notification_batches", "user_fcm_tokens", "user_profiles", "company_profiles", "users").forEach { jdbc.update("delete from $it") }
        val now = LocalDateTime.now(clock)
        listOf("백엔드/서버 개발", "마케터", "개발 직무 전체", "직무 무관").forEachIndexed { index, wishJob ->
            val userId = tx.execute { users.append(UserAppendDto(970001L + index, now)).userId }!!
            jdbc.update("insert into user_profiles (user_id, wish_job, last_synced_at, created_at, updated_at) values (?, ?, ?, ?, ?)",
                userId, wishJob, now, now, now)
            tx.executeWithoutResult { tokens.register(userId, UserFcmTokenRegisterDto("token-$index", UUID.randomUUID()), now) }
        }
        tx.execute { users.appendCompany(CompanyAccountAppendDto("ads-company@example.com", "encoded", now)) }
    }

    @Test
    @DisplayName("희망 직무 발송은 고른 직무와 희망 직무가 겹치는 일반 회원에게만 기기마다 보내고 직무 무관 회원은 뺀다")
    fun `희망 직무가 겹치는 회원에게만 보낸다`() {
        // when
        val result = send(mapOf("targetType" to "JOB_ROLES", "jobRoles" to listOf("백엔드/서버 개발")))

        // then: 백엔드를 고른 회원과 개발 직무 전체를 고른 회원 두 명
        assertEquals("DIRECT", result["deliveryTargetType"].asText())
        assertEquals(2, result["targetUserCount"].asInt())
        assertEquals(2, result["enqueuedCount"].asInt())
        assertEquals(setOf("백엔드/서버 개발", "개발 직무 전체"), recipientWishJobs())
        val batchId = result["batchId"].asText()
        mvc.perform(get("/api/v1/admin/notification-batches/$batchId").with(authentication(admin())))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.targetJobRoles[0]").value("백엔드/서버 개발"))
    }

    @Test
    @DisplayName("희망 직무 발송에 목록에 없는 직무나 직무 무관을 보내면 400으로 거부한다")
    fun `모르는 직무는 거부한다`() {
        listOf(listOf("없는 직무"), listOf("직무 무관"), emptyList()).forEach { roles ->
            perform(mapOf("targetType" to "JOB_ROLES", "jobRoles" to roles), status().isBadRequest)
        }
    }

    @Test
    @DisplayName("광고성 전체 발송은 토픽을 쓰지 않고 마케팅 수신 동의한 일반 회원에게만 보내며, 기업 회원과 미동의 회원은 동의 안 함으로 센다")
    fun `광고성 발송은 동의자에게만 기기마다 보낸다`() {
        // given: 렛츠커리어 ID 970002(마케터)만 동의
        Mockito.`when`(marketingAgreements.readAgreedUserIds(anyCollection())).thenReturn(setOf(970002L))

        // when
        val result = send(mapOf("targetType" to "ALL", "advertisement" to true))

        // then
        assertEquals("DIRECT", result["deliveryTargetType"].asText())
        assertEquals(true, result["advertisement"].asBoolean())
        assertEquals(5, result["targetUserCount"].asInt())
        assertEquals(1, result["recipientUserCount"].asInt())
        assertEquals(4, result["noMarketingConsentUserCount"].asInt())
        assertEquals(setOf("마케터"), recipientWishJobs())
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications where recipient_type = 'TOPIC'", Int::class.java))
    }

    @Test
    @DisplayName("광고성 희망 직무 발송은 직무가 겹치는 회원 중 동의자에게만 보낸다")
    fun `광고성 희망 직무 발송은 두 조건을 모두 만족하는 회원에게 보낸다`() {
        // given: 백엔드(970001)와 마케터(970002)가 동의, 개발 직무 전체(970003)는 미동의
        Mockito.`when`(marketingAgreements.readAgreedUserIds(anyCollection())).thenReturn(setOf(970001L, 970002L))

        // when
        val result = send(mapOf("targetType" to "JOB_ROLES", "jobRoles" to listOf("개발 직무 전체"), "advertisement" to true))

        // then
        assertEquals(2, result["targetUserCount"].asInt())
        assertEquals(1, result["noMarketingConsentUserCount"].asInt())
        assertEquals(setOf("백엔드/서버 개발"), recipientWishJobs())
    }

    @Test
    @DisplayName("마케팅 수신 동의를 확인하지 못하면 광고성 발송을 503으로 거부하고 아무것도 적재하지 않는다")
    fun `동의 확인 실패는 등록을 거부한다`() {
        // given
        Mockito.`when`(marketingAgreements.readAgreedUserIds(anyCollection()))
            .thenThrow(InternalServerException(AdminNotificationBatchErrorCode.MARKETING_AGREEMENT_UNAVAILABLE))

        // when
        perform(mapOf("targetType" to "GENERAL_MEMBERS", "advertisement" to true), status().isServiceUnavailable)

        // then
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_batches", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
    }

    @Test
    @DisplayName("기업 회원 대상 광고성 발송은 동의 항목이 없어 400으로 거부한다")
    fun `기업 회원 광고성 발송은 거부한다`() {
        perform(mapOf("targetType" to "COMPANY_MEMBERS", "advertisement" to true), status().isBadRequest)
        Mockito.verifyNoInteractions(marketingAgreements)
    }

    @Test
    @DisplayName("희망 직무 선택지 조회는 직군마다 직군 전체 선택지와 직무 목록을 돌려준다")
    fun `희망 직무 선택지를 조회한다`() {
        mvc.perform(get("/api/v1/admin/notification-batches/job-role-options").with(authentication(admin())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[1].jobField").value("마케팅"))
            .andExpect(jsonPath("$.data[1].allRolesOption").value("마케팅 직무 전체"))
            .andExpect(jsonPath("$.data[1].jobRoles[0]").value("마케터"))
    }

    private fun recipientWishJobs(): Set<String> = jdbc.queryForList(
        "select p.wish_job from user_notifications n join user_profiles p on p.user_id = n.recipient_user_id", String::class.java,
    ).toSet()

    private fun admin() = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun body(fields: Map<String, Any>) = mapOf("title" to "공지", "body" to "본문",
        "data" to mapOf("url" to "https://example.com/notice")) + fields

    private fun perform(fields: Map<String, Any>, expected: ResultMatcher) {
        mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(admin()))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body(fields))))
            .andExpect(expected)
    }

    private fun send(fields: Map<String, Any>): JsonNode {
        val response = mvc.perform(post("/api/v1/admin/notification-batches").with(authentication(admin()))
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body(fields))))
            .andExpect(status().isCreated).andReturn()
        return mapper.readTree(response.response.contentAsByteArray)["data"]
    }
}
