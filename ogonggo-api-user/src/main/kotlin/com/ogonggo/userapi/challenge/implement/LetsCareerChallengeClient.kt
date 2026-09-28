package com.ogonggo.userapi.challenge.implement

import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import com.ogonggo.userapi.challenge.implement.dto.RecommendedChallengeDto
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime
import java.util.Optional

@Component
class LetsCareerChallengeClient(
    private val letsCareerRestClient: RestClient,
    private val properties: LetsCareerProperties,
) {

    /**
     * 렛츠커리어가 고른 추천 챌린지를 가져온다. 몇 개를 어떻게 고를지는 렛츠커리어가 정한다.
     *
     * 가져오지 못하면 예외 대신 빈 목록을 반환한다.
     * 추천은 화면의 한 구역일 뿐이라 렛츠커리어 장애가 오공고 요청을 실패로 만들면 안 된다.
     *
     * @param letsCareerUserId 사용자에 맞춘 추천에 쓸 렛츠커리어 사용자 id. 비로그인·기업 회원이면 null
     */
    fun readRecommended(letsCareerUserId: Long?): List<RecommendedChallengeDto> {
        val response = try {
            letsCareerRestClient.get()
                .uri { builder ->
                    builder.path(RECOMMEND_PATH)
                        .queryParamIfPresent("userId", Optional.ofNullable(letsCareerUserId))
                        .build()
                }
                .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
                .retrieve()
                .body(object : ParameterizedTypeReference<LetsCareerApiResponse<RecommendResponse>>() {})
        } catch (exception: RestClientException) {
            // 4xx·5xx와 통신 실패가 모두 여기로 온다. 추천 구역만 비우고 요청은 성공으로 둔다.
            log.warn("렛츠커리어 추천 챌린지 조회에 실패했습니다. letsCareerUserId={}", letsCareerUserId, exception)
            return emptyList()
        }

        return response?.data?.challengeList.orEmpty().mapNotNull(ChallengeResponse::toDtoOrNull)
    }

    companion object {
        private const val RECOMMEND_PATH = "/api/v1/internal/challenges/recommend"
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private val log = LoggerFactory.getLogger(LetsCareerChallengeClient::class.java)
    }
}

internal data class LetsCareerApiResponse<T>(
    val status: Int?,
    val message: String?,
    val data: T?,
)

internal data class RecommendResponse(
    val challengeList: List<ChallengeResponse>?,
)

/** 렛츠커리어 `ChallengeRecommendVo`의 모양이다. 오공고가 쓰지 않는 필드는 받지 않는다. */
internal data class ChallengeResponse(
    val id: Long?,
    val title: String?,
    val shortDesc: String?,
    val thumbnail: String?,
    val beginning: LocalDateTime?,
    val deadline: LocalDateTime?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
) {
    /** 식별자나 제목이 없으면 카드를 그릴 수 없으므로 그 항목만 뺀다. */
    fun toDtoOrNull(): RecommendedChallengeDto? {
        if (id == null || title.isNullOrBlank()) return null
        return RecommendedChallengeDto(
            challengeId = id,
            title = title,
            shortDescription = shortDesc,
            thumbnailUrl = thumbnail,
            recruitmentStartAt = beginning,
            recruitmentEndAt = deadline,
            programStartAt = startDate,
            programEndAt = endDate,
        )
    }
}
