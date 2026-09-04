package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.health.AdminHealthController
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 관리자 API 체인도 `anyRequest().denyAll()`로 끝나므로 사용자 API와 같은 회귀를 각각 고정한다.
 * 두 모듈은 독립 배포 경계라 한쪽 테스트가 다른 쪽을 보장하지 않는다.
 */
@WebMvcTest(controllers = [AdminHealthController::class])
@Import(AdminSecurityConfiguration::class)
@TestPropertySource(properties = ["ogonggo.admin.internal.api-key=test-internal-api-key"])
class AdminCorsConfigurationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `허용한 오리진의 preflight는 CORS 헤더와 함께 통과한다`() {
        mockMvc.perform(
            options("/api/v1/internal/jobs")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"))
    }

    @Test
    fun `허용하지 않은 오리진의 preflight는 거부한다`() {
        mockMvc.perform(
            options("/api/v1/internal/jobs")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST"),
        )
            .andExpect(status().isForbidden)
    }

    /** 로컬은 포트를 열어두었으므로 목록에 없던 포트도 그대로 통과해야 한다. */
    @ParameterizedTest
    @ValueSource(strings = ["http://localhost:3000", "http://localhost:5173", "http://localhost:64321"])
    fun `로컬은 어떤 포트에서 와도 허용한다`(origin: String) {
        mockMvc.perform(
            get("/health")
                .header("Origin", origin),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", origin))
    }

    /** 포트를 열었다고 해서 호스트까지 열린 것은 아니다. */
    @Test
    fun `localhost를 흉내낸 다른 호스트는 거부한다`() {
        mockMvc.perform(
            get("/health")
                .header("Origin", "http://localhost.evil.com:3000"),
        )
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }

    companion object {
        private const val ALLOWED_ORIGIN = "https://www.ogonggo.co.kr"
    }
}
