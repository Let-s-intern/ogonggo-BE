package com.ogonggo.userapi.user.implement

import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class LetsCareerUserClientTest {

    private val builder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = LetsCareerUserClient(builder.build(), LetsCareerProperties(BASE_URL, API_KEY))

    @Test
    fun `내부 API 키와 기존·새 비밀번호를 실어 렛츠커리어에 변경을 전달한다`() {
        // given
        server.expect(requestTo("$BASE_URL/api/v1/internal/users/4821/password"))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(header("X-Internal-Api-Key", API_KEY))
            .andExpect(content().json("""{"password":"old-password!","newPassword":"new-password!"}"""))
            .andRespond(withSuccess("""{"status":200,"message":"ok","data":null}""", MediaType.APPLICATION_JSON))

        // when
        client.changePassword(4821L, "old-password!", "new-password!")

        // then
        server.verify()
    }

    @Test
    fun `렛츠커리어의 400 오류 코드를 오공고 오류로 옮긴다`() {
        mapOf(
            "MISMATCH_PASSWORD" to UserErrorCode.CURRENT_PASSWORD_MISMATCH,
            "INVALID_PASSWORD" to UserErrorCode.INVALID_NEW_PASSWORD,
            "INVALID_AUTH_PROVIDER_KAKAO" to UserErrorCode.SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE,
            "INVALID_AUTH_PROVIDER_GOOGLE" to UserErrorCode.SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE,
        ).forEach { (letsCareerCode, expected) ->
            server.reset()
            server.expect(requestTo("$BASE_URL/api/v1/internal/users/4821/password"))
                .andRespond(errorBody(HttpStatus.BAD_REQUEST, letsCareerCode))

            val exception = assertThrows(BusinessException::class.java) {
                client.changePassword(4821L, "old-password!", "new-password!")
            }

            assertEquals(expected, exception.errorCode, letsCareerCode)
        }
    }

    @Test
    fun `사용자가 고칠 수 없는 실패는 렛츠커리어 연동 실패로 알린다`() {
        listOf(
            errorBody(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"),
            errorBody(HttpStatus.FORBIDDEN, "FORBIDDEN"),
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR),
        ).forEach { response ->
            server.reset()
            server.expect(requestTo("$BASE_URL/api/v1/internal/users/4821/password")).andRespond(response)

            val exception = assertThrows(BusinessException::class.java) {
                client.changePassword(4821L, "old-password!", "new-password!")
            }

            assertEquals(AuthErrorCode.LETSCAREER_UNAVAILABLE, exception.errorCode)
        }
    }

    private fun errorBody(status: HttpStatus, code: String) = withStatus(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body("""{"status":${status.value()},"code":"$code","message":"렛츠커리어 메시지"}""")

    companion object {
        private const val BASE_URL = "http://letscareer.test"
        private const val API_KEY = "internal-key"
    }
}
