package com.ogonggo.userapi.enumeration.presentation

import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.enumeration.business.UserEnumService
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserEnumController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserEnumControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userEnumService: UserEnumService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 enum 이름별 선택지를 이름과 라벨, 상위 값으로 조회한다`() {
        // given
        Mockito.`when`(userEnumService.getEnums()).thenReturn(
            mapOf(
                "EmploymentType" to listOf(EnumOption(name = "FULL_TIME", desc = "정규직")),
                "SubRegion" to listOf(EnumOption(name = "SEOUL_GANGNAM_GU", desc = "강남구", parent = "SEOUL")),
            ),
        )

        // when & then
        mockMvc.perform(get("/api/v1/enums"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.EmploymentType[0].name").value("FULL_TIME"))
            .andExpect(jsonPath("$.data.EmploymentType[0].desc").value("정규직"))
            .andExpect(jsonPath("$.data.EmploymentType[0].code").doesNotExist())
            .andExpect(jsonPath("$.data.EmploymentType[0].parent").isEmpty)
            .andExpect(jsonPath("$.data.SubRegion[0].parent").value("SEOUL"))
    }
}
