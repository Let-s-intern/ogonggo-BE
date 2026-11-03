package com.ogonggo.adminapi.notificationbatch.implement

import com.ogonggo.adminapi.notificationbatch.error.AdminNotificationBatchErrorCode
import com.ogonggo.core.error.InternalServerException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

/**
 * 관리자 API가 렛츠커리어 내부 API를 부를 때 쓰는 설정. 사용자 API와 같은 키(`ogonggo.letscareer`)를 쓴다.
 * 광고성 발송에만 필요하므로 비어 있어도 기동은 하고, 광고성 발송을 등록할 때 503으로 거부한다.
 */
@ConfigurationProperties("ogonggo.letscareer")
data class AdminLetsCareerProperties(
    val baseUrl: String = "",
    val internalApiKey: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
) {
    val configured: Boolean get() = baseUrl.isNotBlank() && internalApiKey.isNotBlank()

    override fun toString(): String = "AdminLetsCareerProperties(baseUrl=$baseUrl, internalApiKey=${if (internalApiKey.isBlank()) "<empty>" else "<set>"})"
}

/**
 * 렛츠커리어에 회원의 마케팅 수신 동의 여부를 묻는다(`POST /api/v1/internal/users/marketing-agreements`).
 * 동의 값은 렛츠커리어 회원 테이블에만 있고, 오공고 가입 화면의 선택 동의도 렛츠커리어로 저장된다.
 * 발송을 등록하는 순간마다 물으므로 렛츠커리어에서 철회하면 다음 발송부터 바로 빠진다.
 */
@Component
class LetsCareerMarketingAgreementClient internal constructor(
    @Qualifier(LETSCAREER_REST_CLIENT) private val restClient: RestClient,
    private val properties: AdminLetsCareerProperties,
) {
    /** 렛츠커리어 회원 ID 중 마케팅 수신에 동의한 ID. 1,000개씩 나눠 묻는다. 확인하지 못하면 503으로 거부한다. */
    fun readAgreedUserIds(letsCareerUserIds: Collection<Long>): Set<Long> {
        if (letsCareerUserIds.isEmpty()) return emptySet()
        if (!properties.configured) {
            log.error("렛츠커리어 연결 설정이 없어 마케팅 수신 동의를 확인하지 못했습니다.")
            throw InternalServerException(AdminNotificationBatchErrorCode.MARKETING_AGREEMENT_UNAVAILABLE)
        }
        return letsCareerUserIds.distinct().chunked(CHUNK_SIZE).flatMapTo(mutableSetOf()) { chunk ->
            val response = try {
                restClient.post().uri(PATH)
                    .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
                    .body(MarketingAgreementRequest(chunk))
                    .retrieve()
                    .body(MarketingAgreementApiResponse::class.java)
            } catch (exception: RestClientException) {
                log.error("렛츠커리어 마케팅 수신 동의 조회에 실패했습니다. requested={}, errorType={}", chunk.size, exception::class.simpleName)
                throw InternalServerException(AdminNotificationBatchErrorCode.MARKETING_AGREEMENT_UNAVAILABLE)
            }
            response?.data?.agreedUserIds ?: run {
                log.error("렛츠커리어 마케팅 수신 동의 응답에 data가 없습니다. requested={}", chunk.size)
                throw InternalServerException(AdminNotificationBatchErrorCode.MARKETING_AGREEMENT_UNAVAILABLE)
            }
        }
    }

    internal data class MarketingAgreementRequest(val userIds: List<Long>)
    internal data class MarketingAgreementData(val agreedUserIds: List<Long>?)
    internal data class MarketingAgreementApiResponse(val status: Int?, val message: String?, val data: MarketingAgreementData?)

    companion object {
        const val LETSCAREER_REST_CLIENT = "adminLetsCareerRestClient"
        private const val PATH = "/api/v1/internal/users/marketing-agreements"
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private const val CHUNK_SIZE = 1000
        private val log = LoggerFactory.getLogger(LetsCareerMarketingAgreementClient::class.java)
    }
}
