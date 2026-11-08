package com.ogonggo.adminapi.advertisementexposure.presentation

import com.ogonggo.adminapi.advertisementexposure.business.AdminAdvertisementExposureResult
import com.ogonggo.adminapi.advertisementexposure.business.AdminAdvertisementExposureService
import com.ogonggo.adminapi.advertisementexposure.business.CreateAdminAdvertisementExposureCommand
import com.ogonggo.adminapi.advertisementexposure.business.CreateAdminAdvertisementExposureResult
import com.ogonggo.adminapi.advertisementexposure.business.UpdateAdminAdvertisementExposureCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdvertisementExposureController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertisementExposureControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertisementExposureService: AdminAdvertisementExposureService

    @Test
    fun `관리자 토큰이 없으면 노출 목록과 등록을 호출할 수 없다`() {
        mockMvc.perform(get(PATH).queryParam("productId", "11"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BANNER_REQUEST),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(put("$PATH/501").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(patch("$PATH/501/exposed").contentType(MediaType.APPLICATION_JSON).content("{\"exposed\":false}"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(post("$PATH/501/move").contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"UP\"}"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(delete("$PATH/501"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `관리자 권한이 없으면 노출 API는 403이다`() {
        listOf(
            post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST),
            put("$PATH/501").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST),
            patch("$PATH/501/exposed").contentType(MediaType.APPLICATION_JSON).content("{\"exposed\":false}"),
            post("$PATH/501/move").contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"UP\"}"),
            delete("$PATH/501"),
        ).forEach { request ->
            mockMvc.perform(request.with(user("member")))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        }

        Mockito.verifyNoInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `노출 목록은 상품 필수 필터와 선택 계약 필터를 전달하고 결과를 배열로 반환한다`() {
        Mockito.`when`(adminAdvertisementExposureService.getExposures(11, 1001))
            .thenReturn(listOf(exposureResult()))

        mockMvc.perform(admin(get(PATH).queryParam("productId", "11").queryParam("contractId", "1001")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(501))
            .andExpect(jsonPath("$.data[0].productId").value(11))
            .andExpect(jsonPath("$.data[0].contractId").value(1001))
            .andExpect(jsonPath("$.data[0].advertiserCompanyName").value("샘플기업"))
            .andExpect(jsonPath("$.data[0].displayOrder").value(1))
            .andExpect(jsonPath("$.data[0].exposed").value(false))
            .andExpect(jsonPath("$.data[0].targetType").value(org.hamcrest.Matchers.nullValue()))

        Mockito.verify(adminAdvertisementExposureService).getExposures(11, 1001)
    }

    @Test
    fun `알 수 없는 상품 필터는 빈 배열을 반환하고 양수 ID를 요구한다`() {
        Mockito.`when`(adminAdvertisementExposureService.getExposures(999, null)).thenReturn(emptyList())

        mockMvc.perform(admin(get(PATH).queryParam("productId", "999")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)

        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(admin(get(PATH).queryParam("productId", "0")))
            .andExpect(status().isBadRequest)
        mockMvc.perform(admin(get(PATH).queryParam("productId", "11").queryParam("contractId", "-1")))
            .andExpect(status().isBadRequest)

        Mockito.verify(adminAdvertisementExposureService).getExposures(999, null)
        Mockito.verifyNoMoreInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `배너 노출 등록은 평탄화 입력을 전달하고 생성 ID를 201로 반환한다`() {
        Mockito.`when`(
            adminAdvertisementExposureService.createExposure(
                CreateAdminAdvertisementExposureCommand(
                    productId = 11,
                    contractId = 1001,
                    name = "샘플기업 인턴 채용",
                    imageUrl = "https://example.com/assets/banner.png",
                    linkUrl = "/jobs/5501",
                    targetType = null,
                    targetId = null,
                    adLabel = true,
                    startAt = LocalDateTime.parse("2026-11-01T00:00:00"),
                    endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
                    exposed = true,
                ),
            ),
        ).thenReturn(CreateAdminAdvertisementExposureResult(501))

        mockMvc.perform(
            admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(501))

        Mockito.verify(adminAdvertisementExposureService).createExposure(
            CreateAdminAdvertisementExposureCommand(
                productId = 11,
                contractId = 1001,
                name = "샘플기업 인턴 채용",
                imageUrl = "https://example.com/assets/banner.png",
                linkUrl = "/jobs/5501",
                targetType = null,
                targetId = null,
                adLabel = true,
                startAt = LocalDateTime.parse("2026-11-01T00:00:00"),
                endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
                exposed = true,
            ),
        )
    }

    @Test
    fun `노출 등록은 양수 정수 ID와 명시적 boolean을 요구한다`() {
        listOf(
            BANNER_REQUEST.replace("\"productId\":11", "\"productId\":0"),
            BANNER_REQUEST.replace("\"productId\":11", "\"productId\":11.5"),
            BANNER_REQUEST.replace("\"productId\":11", "\"productId\":\"11\""),
            BANNER_REQUEST.replace("\"contractId\":1001", "\"contractId\":1.5"),
            BANNER_REQUEST.replace("\"adLabel\":true,", ""),
            BANNER_REQUEST.replace("\"adLabel\":true", "\"adLabel\":\"true\""),
            BANNER_REQUEST.replace("\"exposed\":true", "\"exposed\":null"),
            BANNER_REQUEST.replace("\"targetType\":null", "\"targetType\":1"),
            BANNER_REQUEST.replace("\"exposed\":true", "\"exposed\":true,\"displayOrder\":1"),
        ).forEach { body ->
            mockMvc.perform(admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `상품 계약과 URL 정책 오류를 각 400 404 응답으로 전달한다`() {
        val failures = listOf(
            InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE),
            InvalidRequestFieldException("imageUrl", "HTTP 또는 HTTPS 절대 URL이어야 합니다."),
            EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND),
            EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND),
        ).iterator()
        Mockito.`when`(
            adminAdvertisementExposureService.createExposure(
                Mockito.any(CreateAdminAdvertisementExposureCommand::class.java) ?: exposureCommand(),
            ),
        )
            .thenAnswer { throw failures.next() }

        mockMvc.perform(admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE"))
        mockMvc.perform(admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("[imageUrl] HTTP 또는 HTTPS 절대 URL이어야 합니다."))
        mockMvc.perform(admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
        mockMvc.perform(admin(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BANNER_REQUEST)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_NOT_FOUND"))

        Mockito.verify(adminAdvertisementExposureService, Mockito.times(4))
            .createExposure(Mockito.any(CreateAdminAdvertisementExposureCommand::class.java) ?: exposureCommand())
    }

    @Test
    fun `노출 전체 수정은 상품 ID를 받지 않고 변경 가능 필드만 전달하고 null data로 응답한다`() {
        mockMvc.perform(admin(put("$PATH/501").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))

        Mockito.verify(adminAdvertisementExposureService).updateExposure(
            UpdateAdminAdvertisementExposureCommand(
                exposureId = 501,
                contractId = null,
                name = "샘플기업 인턴 채용",
                imageUrl = "https://example.com/assets/banner.png",
                linkUrl = "/jobs/5501",
                targetType = null,
                targetId = null,
                adLabel = true,
                startAt = null,
                endAt = null,
            ),
        )
    }

    @Test
    fun `노출 수정에서 생략한 nullable 필드는 전체 교체 값 null로 전달한다`() {
        mockMvc.perform(
            admin(
                put("$PATH/501")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"샘플 배너","imageUrl":"https://example.com/banner.png","linkUrl":"/jobs/5501","adLabel":true}"""),
            ),
        ).andExpect(status().isOk)

        Mockito.verify(adminAdvertisementExposureService).updateExposure(
            UpdateAdminAdvertisementExposureCommand(
                exposureId = 501,
                contractId = null,
                name = "샘플 배너",
                imageUrl = "https://example.com/banner.png",
                linkUrl = "/jobs/5501",
                targetType = null,
                targetId = null,
                adLabel = true,
                startAt = null,
                endAt = null,
            ),
        )
    }

    @Test
    fun `노출 수정은 adLabel을 필수로 받고 productId exposed displayOrder와 알 수 없는 필드를 거부한다`() {
        listOf(
            """{"name":"배너"}""",
            UPDATE_REQUEST.replace("\"adLabel\":true", "\"adLabel\":null"),
            UPDATE_REQUEST.replace("\"contractId\":null", "\"contractId\":1.5"),
            UPDATE_REQUEST.replace("\"targetId\":null", "\"targetId\":\"12\""),
            UPDATE_REQUEST.replace("\"endAt\":null", "\"endAt\":\"2026-11-01T00:00:00\",\"productId\":11"),
            UPDATE_REQUEST.replace("\"endAt\":null", "\"endAt\":null,\"exposed\":true"),
            UPDATE_REQUEST.replace("\"endAt\":null", "\"endAt\":null,\"displayOrder\":1"),
        ).forEach { body ->
            mockMvc.perform(admin(put("$PATH/501").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        listOf("0", "-1").forEach { id ->
            mockMvc.perform(admin(put("$PATH/$id").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST)))
                .andExpect(status().isBadRequest)
        }

        Mockito.verifyNoInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `켜기 끄기는 boolean만 받고 성공 데이터가 null이다`() {
        mockMvc.perform(admin(patch("$PATH/501/exposed").contentType(MediaType.APPLICATION_JSON).content("{\"exposed\":false}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        Mockito.verify(adminAdvertisementExposureService).changeExposed(501, false)

        listOf("{}", "{\"exposed\":null}", "{\"exposed\":\"false\"}", "{\"exposed\":true,\"displayOrder\":2}").forEach { body ->
            mockMvc.perform(admin(patch("$PATH/501/exposed").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest)
        }
        listOf("0", "-1").forEach { id ->
            mockMvc.perform(admin(patch("$PATH/$id/exposed").contentType(MediaType.APPLICATION_JSON).content("{\"exposed\":false}")))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoMoreInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `이동 명령은 UP DOWN만 받고 경계 이동을 성공 데이터 null로 응답한다`() {
        mockMvc.perform(admin(post("$PATH/501/move").contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"UP\"}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        Mockito.verify(adminAdvertisementExposureService).moveExposure(501, com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection.UP)

        listOf("{}", "{\"direction\":null}", "{\"direction\":\"SIDEWAYS\"}", "{\"direction\":\"DOWN\",\"contractId\":1001}").forEach { body ->
            mockMvc.perform(admin(post("$PATH/501/move").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest)
        }
        listOf("0", "-1").forEach { id ->
            mockMvc.perform(admin(post("$PATH/$id/move").contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"DOWN\"}")))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoMoreInteractions(adminAdvertisementExposureService)
    }

    @Test
    fun `노출 삭제는 본문 없는 200 null 응답이고 ID는 양수여야 한다`() {
        mockMvc.perform(admin(delete("$PATH/501")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        Mockito.verify(adminAdvertisementExposureService).deleteExposure(501)

        listOf("0", "-1").forEach { id ->
            mockMvc.perform(admin(delete("$PATH/$id")))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoMoreInteractions(adminAdvertisementExposureService)
    }

    private fun exposureCommand() = CreateAdminAdvertisementExposureCommand(
        productId = 11,
        contractId = 1001,
        name = "샘플기업 인턴 채용",
        imageUrl = "https://example.com/assets/banner.png",
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = LocalDateTime.parse("2026-11-01T00:00:00"),
        endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
        exposed = true,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun exposureResult() = AdminAdvertisementExposureResult(
        id = 501,
        productId = 11,
        contractId = 1001,
        name = "샘플기업 인턴 채용",
        imageUrl = "https://example.com/assets/banner.png",
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = LocalDateTime.parse("2026-11-01T00:00:00"),
        endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
        exposed = false,
        advertiserCompanyName = "샘플기업",
        displayOrder = 1,
        registeredAt = LocalDateTime.parse("2026-11-07T18:10:00"),
        updatedAt = LocalDateTime.parse("2026-11-07T18:10:00"),
    )

    private companion object {
        const val PATH = "/api/v1/admin/advertisement-exposures"
        const val BANNER_REQUEST = """{"productId":11,"contractId":1001,"name":"샘플기업 인턴 채용","imageUrl":"https://example.com/assets/banner.png","linkUrl":"/jobs/5501","targetType":null,"targetId":null,"adLabel":true,"startAt":"2026-11-01T00:00:00","endAt":"2026-11-30T23:59:59","exposed":true}"""
        const val UPDATE_REQUEST = """{"contractId":null,"name":"샘플기업 인턴 채용","imageUrl":"https://example.com/assets/banner.png","linkUrl":"/jobs/5501","targetType":null,"targetId":null,"adLabel":true,"startAt":null,"endAt":null}"""
    }
}
