package com.ogonggo.adminapi.auth.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter.Companion.INTERNAL_API_KEY_HEADER
import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.presentation.AdminJobController
import com.ogonggo.adminapi.job.presentation.AdminJobFixtures
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@WebMvcTest(controllers = [AdminJobController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
@TestPropertySource(
    properties = [
        "ogonggo.auth.jwt.secret=$JWT_SECRET",
        "ogonggo.admin.internal.api-key=$INTERNAL_API_KEY",
    ],
)
class AdminAuthenticationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminJobService: AdminJobService

    @Test
    fun `토큰이 없으면 401로 응답한다`() {
        mockMvc.perform(get(JOB_PATH))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(adminAuthService, adminJobService)
    }

    @Test
    fun `서명이 다르거나 만료되었거나 리프레시 토큰이면 401로 응답한다`() {
        val otherKey = Jwts.SIG.HS512.key().build()

        listOf(
            token(key = otherKey),
            token(expiresAt = Instant.now().minusSeconds(60)),
            token(type = "refresh"),
        ).forEach { invalidToken ->
            mockMvc.perform(get(JOB_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer $invalidToken"))
                .andExpect(status().isUnauthorized)
        }

        Mockito.verifyNoInteractions(adminAuthService, adminJobService)
    }

    @Test
    fun `유효한 토큰이어도 활성 관리자가 아니면 403으로 응답한다`() {
        Mockito.`when`(adminAuthService.isActiveAdmin(USER_ID)).thenReturn(false)

        mockMvc.perform(get(JOB_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `활성 관리자면 콘솔 API를 호출한다`() {
        Mockito.`when`(adminAuthService.isActiveAdmin(USER_ID)).thenReturn(true)
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(get(JOB_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(693))
    }

    /** 크롤러 키는 내부 경로 전용이다. 키가 맞아도 관리자 콘솔은 열리지 않는다. */
    @Test
    fun `내부 API 키로는 관리자 콘솔을 호출할 수 없다`() {
        mockMvc.perform(get(JOB_PATH).header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
            .andExpect(status().isForbidden)

        Mockito.verifyNoInteractions(adminJobService)
    }

    private fun token(
        type: String = "access",
        key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)),
        expiresAt: Instant = Instant.now().plusSeconds(600),
    ): String = Jwts.builder()
        .subject(USER_ID.toString())
        .claim("type", type)
        .issuedAt(Date())
        .expiration(Date.from(expiresAt))
        .signWith(key, Jwts.SIG.HS512)
        .compact()

    companion object {
        private const val USER_ID = 17L
        private const val JOB_PATH = "/api/v1/admin/jobs/693"
    }
}

private const val JWT_SECRET =
    "b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA=="
private const val INTERNAL_API_KEY = "test-internal-api-key"
