package com.ogonggo.userapi.user.implement

import com.fasterxml.jackson.annotation.JsonFormat
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime

/**
 * 렛츠커리어가 보관 중인 학력과 희망 조건이다. 최초 가입 시점에 한 번만 가져온다.
 * updatedAt은 렛츠커리어에서 마지막으로 고친 일시이며, 한 번도 고친 적 없으면 null이다.
 */
data class LetsCareerJobProfile(
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
    val updatedAt: LocalDateTime?,
) {
    fun toCommand(): UserProfileJobInfoDto = UserProfileJobInfoDto(
        university = university,
        major = major,
        grade = grade,
        wishField = wishField,
        wishJob = wishJob,
        wishIndustry = wishIndustry,
        wishEmploymentType = wishEmploymentType,
        wishCompany = wishCompany,
    )
}

@Component
class LetsCareerUserClient(
    private val letsCareerRestClient: RestClient,
    private val letsCareerPasswordRestClient: RestClient,
    private val properties: LetsCareerProperties,
) {

    /**
     * 가져오지 못하면 예외 대신 null을 반환한다.
     * 학력과 희망 조건은 로그인의 성공 조건이 아니므로 이 호출이 가입을 실패로 만들면 안 된다.
     */
    fun readJobProfile(letsCareerUserId: Long): LetsCareerJobProfile? {
        val response = try {
            letsCareerRestClient.get()
                .uri(JOB_PROFILE_PATH, letsCareerUserId)
                .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
                .retrieve()
                .body(object : ParameterizedTypeReference<LetsCareerApiResponse<JobProfileResponse>>() {})
        } catch (exception: RestClientException) {
            // 4xx·5xx와 통신 실패가 모두 여기로 온다. 가입을 막지 않고 비어 있는 상태로 둔다.
            log.warn("렛츠커리어 학력·희망 조건 조회에 실패했습니다. letsCareerUserId={}", letsCareerUserId, exception)
            return null
        }

        return response?.data?.toResult()
    }

    /**
     * 오공고에서 고친 학력·희망 조건 전체를 렛츠커리어로 보낸다. 렛츠커리어는 더 나중에 고친 쪽의 값만 남긴다.
     * 실패하면 RestClientException을 그대로 던진다. 아웃박스가 다음 주기에 다시 보낸다.
     * 렛츠커리어에 사용자가 없으면(탈퇴) 보낼 곳이 없으므로 성공으로 본다.
     */
    fun replaceJobProfile(letsCareerUserId: Long, command: LetsCareerJobProfileReplaceCommand) {
        letsCareerRestClient.put()
            .uri(JOB_PROFILE_PATH, letsCareerUserId)
            .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
            .body(command)
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) { _, _ ->
                log.warn("렛츠커리어에 없는 사용자라 학력·희망 조건을 보내지 않습니다. letsCareerUserId={}", letsCareerUserId)
            }
            .toBodilessEntity()
    }

    /**
     * 일반 회원의 비밀번호는 렛츠커리어에 있으므로 변경을 그대로 전달한다.
     * 로그인 이후 렛츠커리어를 부르는 예외이며, 사용자가 직접 요청한 변경이라 실패를 삼키지 않고 알린다.
     *
     * 응답을 받지 못하면(연결 실패·시간 초과) 처음 요청을 포함해 [PASSWORD_CHANGE_MAX_ATTEMPTS]번까지 보낸다.
     * 앞선 요청이 이미 반영됐어도 렛츠커리어가 같은 새 비밀번호의 재시도를 성공으로 처리하므로 안전하다.
     * 응답이 온 실패(4xx·5xx)는 다시 보내도 결과가 같으므로 재시도하지 않는다.
     */
    fun changePassword(letsCareerUserId: Long, currentPassword: String, newPassword: String) {
        val request = PasswordChangeRequest(password = currentPassword, newPassword = newPassword)

        repeat(PASSWORD_CHANGE_MAX_ATTEMPTS) { attempt ->
            try {
                requestPasswordChange(letsCareerUserId, request)
                return
            } catch (exception: ResourceAccessException) {
                log.warn(
                    "렛츠커리어 비밀번호 변경 응답을 받지 못했습니다. letsCareerUserId={}, attempt={}",
                    letsCareerUserId,
                    attempt + 1,
                    exception,
                )
            } catch (exception: HttpClientErrorException) {
                throw passwordChangeFailure(letsCareerUserId, exception)
            } catch (exception: RestClientException) {
                log.error("렛츠커리어 비밀번호 변경 호출에 실패했습니다. letsCareerUserId={}", letsCareerUserId, exception)
                throw InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)
            }
        }

        log.error("렛츠커리어 비밀번호 변경 재시도가 모두 응답 없이 끝났습니다. letsCareerUserId={}", letsCareerUserId)
        throw InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)
    }

    private fun requestPasswordChange(letsCareerUserId: Long, request: PasswordChangeRequest) {
        letsCareerPasswordRestClient.patch()
            .uri(PASSWORD_PATH, letsCareerUserId)
            .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    /** 사용자가 고칠 수 있는 400만 오공고 오류로 옮기고, 그 밖의 4xx는 연동 문제로 본다. */
    private fun passwordChangeFailure(letsCareerUserId: Long, exception: HttpClientErrorException): RuntimeException {
        val code = runCatching { exception.getResponseBodyAs(LetsCareerErrorResponse::class.java)?.code }.getOrNull()
        return when {
            code == "MISMATCH_PASSWORD" -> InvalidValueException(UserErrorCode.CURRENT_PASSWORD_MISMATCH)
            code == "INVALID_PASSWORD" -> InvalidValueException(UserErrorCode.INVALID_NEW_PASSWORD)
            code?.startsWith("INVALID_AUTH_PROVIDER") == true ->
                InvalidValueException(UserErrorCode.SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE)
            else -> {
                log.error(
                    "렛츠커리어가 비밀번호 변경을 거부했습니다. letsCareerUserId={}, status={}, code={}",
                    letsCareerUserId,
                    exception.statusCode,
                    code,
                )
                InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE)
            }
        }
    }

    companion object {
        private const val JOB_PROFILE_PATH = "/api/v1/internal/users/{userId}/job-profile"
        private const val PASSWORD_PATH = "/api/v1/internal/users/{userId}/password"
        private const val PASSWORD_CHANGE_MAX_ATTEMPTS = 3
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private val log = LoggerFactory.getLogger(LetsCareerUserClient::class.java)
    }
}

internal data class LetsCareerApiResponse<T>(
    val status: Int?,
    val message: String?,
    val data: T?,
)

/** 렛츠커리어 내부 API의 학력·희망 조건 교체 요청이다. null이면 비운다. */
data class LetsCareerJobProfileReplaceCommand(
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
    // 직접 만든 RestClient는 LocalDateTime을 배열로 쓴다. 두 서버가 같은 ISO 문자열로 주고받도록 못 박는다.
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val updatedAt: LocalDateTime,
)

internal data class PasswordChangeRequest(
    val password: String,
    val newPassword: String,
)

internal data class LetsCareerErrorResponse(
    val status: Int?,
    val code: String?,
    val message: String?,
)

internal data class JobProfileResponse(
    val userId: Long?,
    val university: String?,
    val major: String?,
    /** 오공고가 모르는 값이 와도 파싱이 깨지지 않도록 문자열로 받는다. */
    val grade: String?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
    val updatedAt: LocalDateTime?,
) {
    /** 렛츠커리어가 주는 형태를 오공고가 쓰는 형태로 옮긴다. */
    fun toResult(): LetsCareerJobProfile = LetsCareerJobProfile(
        university = university,
        major = major,
        // 렛츠커리어가 오공고에 없는 학년을 추가해도 가입이 막히지 않도록 모르는 값은 비운다.
        grade = grade?.let { name -> UserGrade.entries.find { it.name == name } },
        wishField = wishField,
        wishJob = wishJob,
        wishIndustry = wishIndustry,
        wishEmploymentType = wishEmploymentType,
        wishCompany = wishCompany,
        updatedAt = updatedAt,
    )
}
