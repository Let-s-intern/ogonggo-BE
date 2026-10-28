package com.ogonggo.userapi.companyname.presentation

import com.ogonggo.core.companyname.implement.CompanyNameReader
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.companyname.business.UserCompanyNameService
import com.ogonggo.userapi.config.UserSecurityConfiguration
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

@WebMvcTest(controllers = [UserCompanyNameController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class, UserCompanyNameService::class)
class UserCompanyNameControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var companyNameReader: CompanyNameReader

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 회사명을 문자열 목록으로 검색한다`() {
        Mockito.`when`(companyNameReader.search("고위드")).thenReturn(listOf("고위드", "(주)고위드"))

        mockMvc.perform(get(PATH).param("keyword", "고위드"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0]").value("고위드"))
            .andExpect(jsonPath("$.data[1]").value("(주)고위드"))
    }

    @Test
    fun `검색어를 보내지 않으면 빈 목록이다`() {
        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verifyNoInteractions(companyNameReader)
    }

    @Test
    fun `검색어가 50자를 넘으면 400으로 응답한다`() {
        mockMvc.perform(get(PATH).param("keyword", "가".repeat(51)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(companyNameReader)
    }

    private companion object {
        const val PATH = "/api/v1/company-names"
    }
}
