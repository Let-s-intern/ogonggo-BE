package com.ogonggo.userapi.user.implement

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.implement.UserProfileJobInfoCommand
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** 렛츠커리어가 보관 중인 학력과 희망 조건이다. 최초 가입 시점에 한 번만 가져온다. */
data class LetsCareerJobProfile(
    val university: String?,
    val major: String?,
    val grade: UserGrade?,
    val wishField: String?,
    val wishJob: String?,
    val wishIndustry: String?,
    val wishEmploymentType: String?,
    val wishCompany: String?,
) {
    fun toCommand(): UserProfileJobInfoCommand = UserProfileJobInfoCommand(
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

interface LetsCareerUserClient {
    /**
     * 가져오지 못하면 예외 대신 null을 반환한다.
     * 학력과 희망 조건은 로그인의 성공 조건이 아니므로 이 호출이 가입을 실패로 만들면 안 된다.
     */
    fun readJobProfile(letsCareerUserId: Long): LetsCareerJobProfile?
}

@Component
internal class LetsCareerUserRestClient(
    private val letsCareerRestClient: RestClient,
    private val properties: LetsCareerProperties,
) : LetsCareerUserClient {

    override fun readJobProfile(letsCareerUserId: Long): LetsCareerJobProfile? {
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

        val data = response?.data ?: return null

        return LetsCareerJobProfile(
            university = data.university,
            major = data.major,
            // 렛츠커리어가 오공고에 없는 학년을 추가해도 가입이 막히지 않도록 모르는 값은 비운다.
            grade = data.grade?.let { name -> UserGrade.entries.firstOrNull { it.name == name } },
            wishField = data.wishField,
            wishJob = data.wishJob,
            wishIndustry = data.wishIndustry,
            wishEmploymentType = data.wishEmploymentType,
            wishCompany = data.wishCompany,
        )
    }

    companion object {
        private const val JOB_PROFILE_PATH = "/api/v1/internal/users/{userId}/job-profile"
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private val log = LoggerFactory.getLogger(LetsCareerUserRestClient::class.java)
    }
}

internal data class LetsCareerApiResponse<T>(
    val status: Int?,
    val message: String?,
    val data: T?,
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
)
