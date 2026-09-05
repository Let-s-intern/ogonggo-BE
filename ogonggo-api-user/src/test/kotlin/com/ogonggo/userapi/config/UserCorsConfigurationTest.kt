package com.ogonggo.userapi.config

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.health.UserHealthController
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 사용자 API 체인은 `anyRequest().denyAll()`로 끝나므로 CORS 배선이 빠지면
 * 브라우저 preflight가 조용히 막힌다. 허용과 차단을 양쪽으로 고정한다.
 */
@WebMvcTest(controllers = [UserHealthController::class])
@Import(UserSecurityConfiguration::class)
class UserCorsConfigurationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var tokenProvider: OgonggoTokenProvider

    @Test
    fun `허용한 오리진의 preflight는 CORS 헤더와 함께 통과한다`() {
        mockMvc.perform(
            options("/api/v1/jobs")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "GET"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"))
    }

    /**
     * Swagger UI를 띄우는 API Gateway 주소다.
     *
     * 같은 오리진인데도 목록에 있어야 한다. 브라우저는 GET이 아닌 요청에는 동일 오리진에도
     * Origin을 붙이고 스프링은 그걸 CORS 요청으로 처리하므로, 빠지면 Swagger의 POST만 403이 된다.
     */
    @Test
    fun `API Gateway 오리진의 POST preflight는 통과한다`() {
        mockMvc.perform(
            options("/api/v1/advertisement-inquiries")
                .header("Origin", API_GATEWAY_ORIGIN)
                .header("Access-Control-Request-Method", "POST"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", API_GATEWAY_ORIGIN))
    }

    /** allowCredentials를 켜도 목록 밖 오리진은 열리지 않는다는 것이 이 테스트의 요지다. */
    @Test
    fun `허용하지 않은 오리진의 preflight는 거부한다`() {
        mockMvc.perform(
            options("/api/v1/jobs")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "GET"),
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
        private const val API_GATEWAY_ORIGIN = "https://qi9peez04m.execute-api.ap-northeast-2.amazonaws.com"
    }
}
