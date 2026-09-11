package com.ogonggo.adminapi.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 실제 컨텍스트에서 스프링 부트가 db 헬스 항목을 그 이름으로 등록하는지 확인한다.
 * 이름이 어긋나면 헬스 체크가 항상 503이 되어 배포가 전부 실패한다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-admin-health;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
class AdminHealthCheckTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `DB에 붙으면 로그인 없이 200과 항목별 상태를 준다`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("ogonggo-api-admin"))
            .andExpect(jsonPath("$.components.db").value("UP"))
            .andExpect(jsonPath("$.components.redis").doesNotExist())
    }
}
