package com.ogonggo.adminapi.announcement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 공지를 노출할 때 푸시를 한 번만, 대상을 정한 공지에만 보낸다. */
@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-announcement-push;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ogonggo.notification.broadcast.enabled=true", "ogonggo.notification.broadcast.topic=ogonggo-all-test",
    "ogonggo.announcement.push.web-base-url=https://dev.example.com/",
])
@AutoConfigureMockMvc
class AdminAnnouncementPushIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val jdbc: JdbcTemplate,
) {

    @BeforeEach
    fun clean() {
        listOf("notification_broadcast_reads", "notifications", "user_notifications", "notification_batches", "announcements")
            .forEach { jdbc.update("delete from $it") }
    }

    @Test
    fun `대상을 정하고 노출로 등록하면 그 대상에게 푸시를 한 번 보내고 발송 묶음을 남긴다`() {
        // when
        val id = create(visibility = "VISIBLE", pushTarget = "ALL", summary = "이번 주 일요일 새벽 점검")

        // then
        assertEquals(1, batchCount())
        val batchId = jdbc.queryForObject("select id from notification_batches", String::class.java)
        assertEquals(batchId, pushBatchId(id))
        assertEquals("ALL", jdbc.queryForObject("select target_type from notification_batches", String::class.java))
        val payload = mapper.readTree(jdbc.queryForObject("select payload_json from notifications", String::class.java))
        assertEquals("가".repeat(100), payload["title"].asText())
        assertEquals("이번 주 일요일 새벽 점검", payload["body"].asText())
        assertEquals("https://dev.example.com/notices/$id", payload.at("/data/url").asText())
    }

    @Test
    fun `숨김으로 등록하면 보내지 않고, 노출로 바꿀 때 한 번 보낸 뒤 다시 노출해도 보내지 않는다`() {
        // given
        val id = create(visibility = "HIDDEN", pushTarget = "GENERAL_MEMBERS")
        assertEquals(0, batchCount())

        // when
        update(id, mapOf("visibility" to "VISIBLE"))
        update(id, mapOf("visibility" to "HIDDEN"))
        update(id, mapOf("visibility" to "VISIBLE", "title" to "고친 제목"))

        // then
        assertEquals(1, batchCount())
        assertEquals("GENERAL_MEMBERS", jdbc.queryForObject("select target_type from notification_batches", String::class.java))
    }

    @Test
    fun `보내지 않음으로 정한 공지는 노출해도 보내지 않는다`() {
        // given
        val id = create(visibility = "HIDDEN", pushTarget = "NONE")

        // when
        update(id, mapOf("visibility" to "VISIBLE"))
        update(id, mapOf("pinned" to true))

        // then
        assertEquals(0, batchCount())
        assertNull(pushBatchId(id))
    }

    @Test
    fun `이미 보낸 공지의 대상을 바꾸면 409이고 다시 보내지 않는다`() {
        // given
        val id = create(visibility = "VISIBLE", pushTarget = "ALL")

        // when
        update(id, mapOf("pushTarget" to "COMPANY_MEMBERS"), status().isConflict)

        // then
        assertEquals(1, batchCount())
    }

    private fun create(visibility: String, pushTarget: String, summary: String? = null): Long {
        val body = mapOf(
            "title" to "가".repeat(120),
            "content" to CONTENT,
            "pinned" to false,
            "visibility" to visibility,
            "pushTarget" to pushTarget,
        ) + (summary?.let { mapOf("summary" to it) } ?: emptyMap())
        val response = mvc.perform(
            post("/api/v1/admin/announcements").with(authentication(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)),
        ).andExpect(status().isCreated).andReturn()
        return mapper.readTree(response.response.contentAsByteArray).at("/data/id").asLong()
    }

    private fun update(id: Long, body: Map<String, Any>, expected: ResultMatcher = status().isOk) {
        mvc.perform(
            patch("/api/v1/admin/announcements/$id").with(authentication(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)),
        ).andExpect(expected)
    }

    private fun batchCount(): Int = jdbc.queryForObject("select count(*) from notification_batches", Int::class.java)!!

    private fun pushBatchId(id: Long): String? =
        jdbc.queryForObject("select push_batch_id from announcements where id = ?", String::class.java, id)

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val ADMIN = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
    }
}
