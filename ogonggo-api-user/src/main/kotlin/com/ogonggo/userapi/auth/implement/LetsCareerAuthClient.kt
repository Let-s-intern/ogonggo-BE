package com.ogonggo.userapi.auth.implement

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.userapi.auth.error.AuthErrorCode
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime

data class LetsCareerUser(
    val userId: Long,
    val email: String?,
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val isAdmin: Boolean,
    val updatedAt: LocalDateTime?,
)

@Component
class LetsCareerAuthClient(
    private val letsCareerRestClient: RestClient,
    private val properties: LetsCareerProperties,
) {

    /** 렛츠커리어 액세스 토큰을 검증하고 연동에 필요한 사용자 정보를 가져온다. */

    fun verify(letsCareerAccessToken: String): LetsCareerUser {
        val response = try {
            letsCareerRestClient.post()
                .uri(VERIFY_PATH)
                .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
                .body(VerifyRequest(letsCareerAccessToken))
                .retrieve()
                .onStatus({ it == HttpStatus.UNAUTHORIZED || it == HttpStatus.NOT_FOUND }) { _, clientResponse ->
                    log.error("렛츠커리어 토큰 검증에 실패했습니다. status={}", clientResponse.statusCode)
                    throw UnauthorizedException(AuthErrorCode.INVALID_LETSCAREER_TOKEN)
                }
                .onStatus({ it == HttpStatus.FORBIDDEN }) { _, _ ->
                    log.error("렛츠커리어 내부 API 키가 거부되었습니다.")
                    throw InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)
                }
                .body(object : ParameterizedTypeReference<LetsCareerApiResponse<VerifyResponse>>() {})
        } catch (exception: RestClientException) {
            log.error("렛츠커리어 서버 호출에 실패했습니다.", exception)
            throw InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)
        }

        val data = response?.data
            ?: throw InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)

        return data.toResult()
    }

    companion object {
        private const val VERIFY_PATH = "/api/v1/internal/auth/verify"
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private val log = LoggerFactory.getLogger(LetsCareerAuthClient::class.java)
    }
}

internal data class VerifyRequest(val accessToken: String)

internal data class LetsCareerApiResponse<T>(
    val status: Int?,
    val message: String?,
    val data: T?,
)

internal data class VerifyResponse(
    val userId: Long,
    val email: String?,
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val isAdmin: Boolean?,
    val updatedAt: LocalDateTime?,
) {
    /** 렛츠커리어가 주는 형태를 오공고가 쓰는 형태로 옮긴다. */
    fun toResult(): LetsCareerUser = LetsCareerUser(
        userId = userId,
        email = email,
        name = name,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
        // 렛츠커리어가 값을 주지 않으면 관리자가 아닌 것으로 본다.
        isAdmin = isAdmin ?: false,
        updatedAt = updatedAt,
    )
}
