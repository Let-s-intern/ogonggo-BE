package com.ogonggo.userapi.health

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 실제 컨텍스트에서 스프링 부트가 db·redis 헬스 항목을 그 이름으로 등록하는지 확인한다.
 * 이름이 어긋나면 헬스 체크가 항상 503이 되어 배포가 전부 실패한다.
 *
 * 테스트 환경에는 Redis가 없으므로 닫힌 포트를 가리키게 해 "붙지 못하는 경우"를 고정한다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-health;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=1",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        // spring.mail.host가 있어야 Spring Boot가 JavaMailSender를 만든다. 헬스 체크는 메일을 보지 않는다.
        "spring.mail.host=localhost",
        "ogonggo.advertisement.slack.inquiry-url=https://hooks.slack.com/services/T000/B000/test",
    ],
)
@AutoConfigureMockMvc
class UserHealthCheckTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `DB에 붙어도 Redis에 붙지 못하면 로그인 없이 503과 항목별 상태만 준다`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.components.db").value("UP"))
            .andExpect(jsonPath("$.components.redis").value("DOWN"))
            // 인증 없이 열린 경로라 접속 주소나 예외 내용이 새면 안 된다.
            .andExpect(content().string(not(containsString("localhost"))))
            .andExpect(content().string(not(containsString("Exception"))))
    }
}
