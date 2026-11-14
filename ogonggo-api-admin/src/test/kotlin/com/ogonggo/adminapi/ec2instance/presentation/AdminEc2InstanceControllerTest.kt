package com.ogonggo.adminapi.ec2instance.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.ec2instance.business.AdminEc2InstanceResult
import com.ogonggo.adminapi.ec2instance.business.AdminEc2InstanceService
import com.ogonggo.adminapi.ec2instance.error.Ec2InstanceErrorCode
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.error.ConflictException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminEc2InstanceController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminEc2InstanceControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminEc2InstanceService: AdminEc2InstanceService

    @Test
    fun `관리자 토큰이 없으면 인스턴스를 켤 수 없다`() {
        mockMvc.perform(post("/api/v1/admin/ec2-instances/crawler/start"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminEc2InstanceService)
    }

    @Test
    fun `인스턴스 이름과 상태를 data에 담고 인스턴스 ID는 내보내지 않는다`() {
        // given
        Mockito.`when`(adminEc2InstanceService.list()).thenReturn(
            listOf(
                AdminEc2InstanceResult(Ec2Instance.REDASH, Ec2InstanceState.RUNNING),
                AdminEc2InstanceResult(Ec2Instance.CRAWLER, Ec2InstanceState.STOPPED),
            ),
        )

        // when & then
        mockMvc.perform(admin(get("/api/v1/admin/ec2-instances")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].instance").value("REDASH"))
            .andExpect(jsonPath("$.data[0].state").value("RUNNING"))
            .andExpect(jsonPath("$.data[1].instance").value("CRAWLER"))
            .andExpect(jsonPath("$.data[1].state").value("STOPPED"))
            .andExpect(jsonPath("$.data[0].instanceId").doesNotExist())
    }

    @Test
    fun `경로의 인스턴스는 대소문자를 가리지 않고 켜기 직후 상태를 돌려준다`() {
        Mockito.`when`(adminEc2InstanceService.start(Ec2Instance.CRAWLER))
            .thenReturn(AdminEc2InstanceResult(Ec2Instance.CRAWLER, Ec2InstanceState.PENDING))

        mockMvc.perform(admin(post("/api/v1/admin/ec2-instances/crawler/start")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.state").value("PENDING"))
    }

    @Test
    fun `REDASH·CRAWLER가 아닌 인스턴스는 400으로 알린다`() {
        mockMvc.perform(admin(post("/api/v1/admin/ec2-instances/db/stop")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[instance] REDASH 또는 CRAWLER만 쓸 수 있습니다."))

        Mockito.verifyNoInteractions(adminEc2InstanceService)
    }

    @Test
    fun `상태를 바꾸는 중이면 409로 알린다`() {
        Mockito.`when`(adminEc2InstanceService.start(Ec2Instance.REDASH)).thenThrow(
            ConflictException(Ec2InstanceErrorCode.EC2_INSTANCE_STATE_CONFLICT),
        )

        mockMvc.perform(admin(post("/api/v1/admin/ec2-instances/REDASH/start")))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EC2_INSTANCE_STATE_CONFLICT"))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
}
