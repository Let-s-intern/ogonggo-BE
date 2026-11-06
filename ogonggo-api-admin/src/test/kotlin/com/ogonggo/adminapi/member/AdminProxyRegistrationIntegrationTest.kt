package com.ogonggo.adminapi.member

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.UserAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.ResultActions
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

/**
 * 운영자가 비즈니스 회원 대신 채용공고·부트캠프를 등록하는 흐름을 실제 저장까지 확인한다.
 * 소유자·검수·노출·대리 등록 기록과 이미지 첨부가 한 요청에서 함께 맞아야 회원 마이페이지와 정리 작업이 어긋나지 않는다.
 */
@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-proxy-registration;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@AutoConfigureMockMvc
@Import(AdminProxyRegistrationIntegrationTest.FixedTime::class)
class AdminProxyRegistrationIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val users: UserAppender,
    private val jdbc: JdbcTemplate,
    private val tx: TransactionTemplate,
) {

    @BeforeEach
    fun reset() {
        listOf("bootcamp_partners", "bootcamp_curriculums", "bootcamps", "jobs", "image_assets", "users")
            .forEach { jdbc.update("delete from $it") }
    }

    @Test
    @DisplayName("대리 등록한 채용공고는 회원 소유의 승인된 공고로 바로 노출되고 대리 등록 기록과 처음 승인 일시가 남는다")
    fun `채용공고를 회원 대신 승인 상태로 등록한다`() {
        // given
        val memberId = companyMember()
        val adminLogo = image("admin-logo", owner = ADMIN_ID)
        val memberCover = image("member-cover", owner = memberId)

        // when
        val response = created(post(jobPath(memberId)), jobBody(logoUrl = adminLogo, coverImageUrl = memberCover))

        // then
        val jobId = response["id"].asLong()
        assertEquals("VISIBLE", response["visibility"].asText())
        assertEquals("APPROVED", response["reviewStatus"].asText())
        assertEquals("COMPANY", response["source"].asText())
        assertEquals(NOW, LocalDateTime.parse(response["proxyRegisteredAt"].asText()))
        assertEquals(ADMIN_ID, response["proxyRegisteredByUserId"].asLong())
        val saved = jdbc.queryForMap("select * from jobs where id = ?", jobId)
        assertEquals(memberId, (saved["owner_user_id"] as Number).toLong())
        assertEquals("PUBLISHED", saved["publication_status"])
        assertEquals(Timestamp.valueOf(NOW), saved["first_approved_at"])
        assertEquals(Timestamp.valueOf(NOW), saved["proxy_registered_at"])
        assertEquals(ADMIN_ID, (saved["proxy_registered_by_user_id"] as Number).toLong())
        assertEquals("ATTACHED", imageStatus("admin-logo"))
        assertEquals("ATTACHED", imageStatus("member-cover"))
        mvc.perform(admin(get("/api/v1/admin/company-members/$memberId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.jobs[0].id").value(jobId))
    }

    @Test
    @DisplayName("비노출로 대리 등록하면 승인된 채로 숨기고 관리자 목록에도 대리 등록 일시가 실린다")
    fun `비노출을 고르면 승인한 뒤 숨긴다`() {
        // given
        val memberId = companyMember()

        // when
        val response = created(post(jobPath(memberId)), jobBody(visibility = "HIDDEN"))

        // then
        assertEquals("HIDDEN", response["visibility"].asText())
        assertEquals("APPROVED", response["reviewStatus"].asText())
        val saved = jdbc.queryForMap("select * from jobs where id = ?", response["id"].asLong())
        assertEquals("HIDDEN", saved["publication_status"])
        val listed = mapper.readTree(
            mvc.perform(admin(get("/api/v1/admin/jobs"))).andExpect(status().isOk).andReturn().response.contentAsByteArray,
        ).at("/data/items/0")
        assertEquals(NOW, LocalDateTime.parse(listed["proxyRegisteredAt"].asText()))
    }

    @Test
    @DisplayName("대리 등록한 부트캠프는 회원 소유의 승인된 과정이며 마감으로 등록하면 지금을 마감 일시로 남긴다")
    fun `부트캠프를 회원 대신 승인 상태로 등록한다`() {
        // given
        val memberId = companyMember()
        val adminImage = image("admin-bootcamp", owner = ADMIN_ID)

        // when
        val response = created(
            post(bootcampPath(memberId)),
            bootcampBody(representativeImageUrl = adminImage, status = "CLOSED", visibility = "HIDDEN"),
        )

        // then
        val bootcampId = response["id"].asLong()
        assertEquals("HIDDEN", response["visibility"].asText())
        assertEquals("APPROVED", response["reviewStatus"].asText())
        assertEquals("COMPANY", response["source"].asText())
        assertEquals("CLOSED", response["status"].asText())
        assertEquals(NOW, LocalDateTime.parse(response["proxyRegisteredAt"].asText()))
        assertEquals(ADMIN_ID, response["proxyRegisteredByUserId"].asLong())
        assertEquals("파트너", response["partners"][0]["name"].asText())
        val saved = jdbc.queryForMap("select * from bootcamps where id = ?", bootcampId)
        assertEquals(memberId, (saved["owner_user_id"] as Number).toLong())
        assertEquals("HIDDEN", saved["publication_status"])
        assertEquals(Timestamp.valueOf(NOW), saved["closed_at"])
        assertEquals(Timestamp.valueOf(NOW), saved["proxy_registered_at"])
        assertEquals("ATTACHED", imageStatus("admin-bootcamp"))
    }

    @Test
    @DisplayName("없거나 비즈니스 회원이 아니면 404, 정지·탈퇴 회원이면 409로 거절하고 아무것도 저장하지 않는다")
    fun `대리 등록 대상 회원을 검증한다`() {
        // given
        val general = tx.execute { users.append(UserAppendDto(IDS.incrementAndGet(), NOW.minusDays(1))).userId }!!
        val suspended = companyMember().also { jdbc.update("update users set status = 'SUSPENDED' where id = ?", it) }
        val withdrawn = companyMember().also { jdbc.update("update users set status = 'WITHDRAWN' where id = ?", it) }

        // when, then
        listOf(general, 999_999L).forEach { userId ->
            send(post(jobPath(userId)), jobBody())
                .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            send(post(bootcampPath(userId)), bootcampBody())
                .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        }
        listOf(suspended, withdrawn).forEach { userId ->
            send(post(jobPath(userId)), jobBody())
                .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("COMPANY_MEMBER_NOT_ACTIVE"))
            send(post(bootcampPath(userId)), bootcampBody())
                .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("COMPANY_MEMBER_NOT_ACTIVE"))
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from jobs", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from bootcamps", Int::class.java))
    }

    @Test
    @DisplayName("기업회원 등록과 같은 칸 검증과 칸 사이 규칙을 따르고, 관리자만 호출할 수 있다")
    fun `기업회원 등록과 같은 요청 검증을 한다`() {
        // given
        val memberId = companyMember()

        // when, then
        send(post(jobPath(memberId)), jobBody(jobRole = "\"GAME_OPERATION\""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.message").value("[jobRole] jobField와 같은 직군의 직무여야 합니다."))
        send(post(jobPath(memberId)), jobBody(title = "\" \""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        send(post(jobPath(memberId)), jobBody(applicationMethod = "\"EMAIL\""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.message").value("[applyEmail] 이메일 지원에는 지원 이메일이 필요합니다."))
        send(post(bootcampPath(memberId)), bootcampBody(partnerName = "중복", secondPartnerName = "중복"))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.message").value("[partners] 중복된 파트너사명은 등록할 수 없습니다."))
        send(post(jobPath(0)), jobBody())
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mvc.perform(post(jobPath(memberId)).contentType(MediaType.APPLICATION_JSON).content(jobBody()))
            .andExpect(status().isUnauthorized)
        mvc.perform(
            post(jobPath(memberId)).contentType(MediaType.APPLICATION_JSON).content(jobBody())
                .with(authentication(UsernamePasswordAuthenticationToken(memberId, null, emptyList()))),
        ).andExpect(status().isForbidden)
        assertEquals(0, jdbc.queryForObject("select count(*) from jobs", Int::class.java))
        assertEquals(0, jdbc.queryForObject("select count(*) from bootcamps", Int::class.java))
    }

    @Test
    @DisplayName("정리 작업이 지운 이미지 URL은 기업회원 등록처럼 IMAGE_ASSET_NOT_AVAILABLE로 거절한다")
    fun `지운 이미지로는 등록하지 않는다`() {
        // given
        val memberId = companyMember()
        val deleted = image("deleted-logo", owner = ADMIN_ID, status = "DELETED")

        // when, then
        send(post(jobPath(memberId)), jobBody(logoUrl = deleted))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("IMAGE_ASSET_NOT_AVAILABLE"))
        assertEquals(0, jdbc.queryForObject("select count(*) from jobs", Int::class.java))
    }

    private fun companyMember(): Long = tx.execute {
        users.appendCompany(CompanyAccountAppendDto("company${IDS.incrementAndGet()}@example.com", "encoded", NOW.minusDays(1))).userId
    }!!

    private fun image(id: String, owner: Long, status: String = "TEMPORARY"): String {
        val url = "https://cdn.example.com/ogonggo/profile/$id.png"
        jdbc.update(
            "insert into image_assets (id, owner_user_id, storage_key, url, mime_type, size, status, created_at, updated_at) " +
                "values (?, ?, ?, ?, 'image/png', 10, ?, ?, ?)",
            id, owner, "ogonggo/profile/$id.png", url, status, NOW.minusHours(1), NOW.minusHours(1),
        )
        return url
    }

    private fun imageStatus(id: String): String =
        jdbc.queryForObject("select status from image_assets where id = ?", String::class.java, id)!!

    private fun jobBody(
        title: String = "\"백엔드 개발자\"",
        jobRole: String = "\"IT_BACKEND\"",
        applicationMethod: String = "null",
        logoUrl: String? = null,
        coverImageUrl: String? = null,
        visibility: String? = null,
    ): String = """
        {
          "companyName": "오공고",
          "title": $title,
          "jobField": "IT_DEVELOPMENT",
          "jobRole": $jobRole,
          "logoUrl": ${logoUrl?.let { "\"$it\"" }},
          "coverImageUrl": ${coverImageUrl?.let { "\"$it\"" }},
          "employmentType": "FULL_TIME",
          "experienceType": "NEWCOMER",
          "recruitmentType": "PERIOD",
          "recruitmentStartAt": "2026-10-20T00:00:00",
          "recruitmentEndAt": "2026-11-30T23:59:00",
          "applicationMethod": $applicationMethod,
          "visibility": ${visibility?.let { "\"$it\"" }}
        }
    """.trimIndent()

    private fun bootcampBody(
        representativeImageUrl: String = "https://example.com/bootcamp.png",
        status: String = "RECRUITING",
        visibility: String? = null,
        partnerName: String = "파트너",
        secondPartnerName: String? = null,
    ): String = """
        {
          "companyName": "오공고 교육",
          "title": "백엔드 부트캠프",
          "programType": "개발",
          "operationType": "ONLINE",
          "recruitmentType": "PERIOD",
          "recruitmentStartAt": "2026-10-20T00:00:00",
          "recruitmentEndAt": "2026-11-30T23:59:00",
          "programStartDate": "2026-12-01",
          "programEndDate": "2027-02-28",
          "tuitionType": "FREE",
          "representativeImageUrl": "$representativeImageUrl",
          "shortDescription": "12주 과정",
          "content": "커리큘럼 안내",
          "applicationMethod": "EXTERNAL_PAGE",
          "applicationUrl": "https://example.com/apply",
          "partners": [{ "partnerName": "$partnerName", "displayOrder": 0 }${secondPartnerName?.let { ", { \"partnerName\": \"$it\", \"displayOrder\": 1 }" } ?: ""}],
          "curriculums": [{ "startWeek": 1, "endWeek": 4, "subtitle": "기초", "displayOrder": 0 }],
          "status": "$status",
          "visibility": ${visibility?.let { "\"$it\"" }}
        }
    """.trimIndent()

    private fun send(request: MockHttpServletRequestBuilder, body: String): ResultActions =
        mvc.perform(admin(request.contentType(MediaType.APPLICATION_JSON).content(body)))

    private fun created(request: MockHttpServletRequestBuilder, body: String): JsonNode = mapper.readTree(
        send(request, body)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andReturn().response.contentAsByteArray,
    )["data"]

    private fun jobPath(userId: Long) = "/api/v1/admin/company-members/$userId/jobs"

    private fun bootcampPath(userId: Long) = "/api/v1/admin/company-members/$userId/bootcamps"

    private fun admin(request: MockHttpServletRequestBuilder) = request.with(authentication(
        UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))),
    ))

    @TestConfiguration
    class FixedTime {
        @Bean
        @Primary
        fun proxyRegistrationClock(): Clock = Clock.fixed(Instant.parse("2026-11-05T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    }

    private companion object {
        const val ADMIN_ID = 42L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 5, 10, 0)
        val IDS = AtomicLong(3_000_000)
    }
}
