package com.ogonggo.userapi.notification.channel.alimtalk

import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.config.UserAlimTalkConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

@Component
internal class NhnAlimTalkClient(
    @Qualifier(UserAlimTalkConfiguration.NHN_ALIMTALK_REST_CLIENT)
    private val restClient: RestClient,
    private val properties: NhnAlimTalkProperties,
) {

    /** provider 응답을 정규화해 상위 sender가 NHN 응답 DTO에 의존하지 않도록 한다. */
    fun send(message: AlimTalkMessage): NhnAlimTalkResult {
        val result = try {
            sendRequest(message)?.toResult() ?: NhnAlimTalkResult.InvalidResponse
        } catch (exception: ResourceAccessException) {
            // 요청이 provider에 도달했는지 알 수 없으므로 UNKNOWN으로 남기고 자동 재발송하지 않는다.
            val safeException = RuntimeException("NHN 알림톡 요청 중 통신 오류가 발생했습니다.").apply {
                stackTrace = exception.stackTrace
            }
            log.error(
                "NHN 알림톡 응답을 받지 못했습니다. templateCode={}, errorType={}",
                message.templateCode,
                exception::class.simpleName,
                safeException,
            )
            NhnAlimTalkResult.Error(
                resultCode = "NHN_TRANSPORT_ERROR",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
                description = "NHN 응답을 받기 전 통신 오류",
            )
        } catch (exception: RestClientResponseException) {
            // HTTP 상태만 저장한다. 응답 본문은 민감 데이터나 변동 메시지가 포함될 수 있어 보존하지 않는다.
            val statusCode = exception.statusCode.value()
            val category = when {
                statusCode == 429 -> NotificationFailureCategory.RATE_LIMITED
                statusCode >= 500 -> NotificationFailureCategory.PROVIDER_UNAVAILABLE
                else -> NotificationFailureCategory.HTTP_ERROR
            }
            NhnAlimTalkResult.Error(
                resultCode = "NHN_HTTP_$statusCode",
                failureCategory = category,
                description = "NHN HTTP 응답 오류 ($statusCode)",
            )
        } catch (exception: RestClientException) {
            // 요청 처리 여부를 단정할 수 없는 오류는 UNKNOWN으로 보존한다.
            log.warn(
                "NHN 알림톡 응답을 분류하지 못했습니다. templateCode={}, errorType={}",
                message.templateCode,
                exception::class.simpleName,
            )
            NhnAlimTalkResult.Error(
                resultCode = "NHN_CLIENT_ERROR",
                failureCategory = NotificationFailureCategory.CLIENT_ERROR,
                description = "NHN 요청 클라이언트 처리 오류",
            )
        }
        logResult(message.templateCode, result)
        return result
    }

    /** HTTP 통신만 담당한다. 저장된 idempotency key는 중복 호출의 provider 보호를 위해 전달한다. */
    private fun sendRequest(message: AlimTalkMessage): NhnAlimTalkResponse? =
        restClient.post()
            .uri("$NHN_BASE_URL/alimtalk/v2.3/appkeys/{appKey}/messages", properties.appKey)
            .contentType(MediaType.APPLICATION_JSON)
            .headers { headers ->
                headers.set(SECRET_KEY_HEADER, properties.secretKey)
                message.idempotencyKey
                    ?.takeIf(String::isNotBlank)
                    ?.let { headers.set(IDEMPOTENCY_KEY_HEADER, it) }
            }
            .body(NhnAlimTalkRequest.from(message, properties.sendKey))
            .retrieve()
            .body(NhnAlimTalkResponse::class.java)

    private fun logResult(templateCode: String, result: NhnAlimTalkResult) {
        // 수신 번호와 payload는 제외하고 템플릿 코드·응답 코드처럼 운영 추적에 필요한 값만 남긴다.
        when (result) {
            is NhnAlimTalkResult.Accepted ->
                log.info("NHN 알림톡 요청이 접수됐습니다. templateCode={}, requestId={}", templateCode, result.requestId)
            is NhnAlimTalkResult.Error ->
                log.warn(
                    "NHN 알림톡 요청 결과. templateCode={}, failureCategory={}, resultCode={}, description={}",
                    templateCode,
                    result.failureCategory,
                    result.resultCode,
                    result.description,
                )
            NhnAlimTalkResult.InvalidResponse ->
                log.warn("NHN 알림톡 응답의 접수 여부를 판단할 수 없습니다. templateCode={}", templateCode)
        }
    }

    private companion object {
        const val NHN_BASE_URL = "https://api-alimtalk.cloud.toast.com"
        const val SECRET_KEY_HEADER = "X-Secret-Key"
        const val IDEMPOTENCY_KEY_HEADER = "X-NC-API-IDEMPOTENCY-KEY"
        val log = LoggerFactory.getLogger(NhnAlimTalkClient::class.java)
    }
}

private data class NhnAlimTalkRequest(
    val senderKey: String,
    val templateCode: String,
    val recipientList: List<NhnRecipient>,
) {
    companion object {
        fun from(message: AlimTalkMessage, senderKey: String): NhnAlimTalkRequest = NhnAlimTalkRequest(
            senderKey = senderKey,
            templateCode = message.templateCode,
            recipientList = listOf(
                NhnRecipient(
                    recipientNo = message.recipientNo,
                    templateParameter = message.templateParameters,
                ),
            ),
        )
    }
}

private data class NhnRecipient(
    val recipientNo: String,
    val templateParameter: Map<String, String>,
)

private data class NhnAlimTalkResponse(
    val header: NhnResponseHeader? = null,
    val message: NhnResponseMessage? = null,
) {
    fun toResult(): NhnAlimTalkResult {
        // 접수 여부를 확인할 필드가 빠졌거나 수신자 결과가 하나로 결정되지 않으면 성공으로 추정하지 않는다.
        val successful = header?.isSuccessful ?: return NhnAlimTalkResult.InvalidResponse
        if (!successful) {
            return rejected(header.resultCode)
        }

        val resultCode = message?.sendResults?.singleOrNull()?.resultCode
            ?: return NhnAlimTalkResult.InvalidResponse
        return if (resultCode == SUCCESS_RESULT_CODE) {
            NhnAlimTalkResult.Accepted(message.requestId)
        } else {
            rejected(resultCode)
        }
    }

    private fun rejected(resultCode: Int?): NhnAlimTalkResult.Error {
        val error = NhnAlimTalkErrorCode.resolve(resultCode)
        return NhnAlimTalkResult.Error(
            resultCode = resultCode?.toString(),
            failureCategory = error.category,
            description = error.description,
        )
    }

    private companion object {
        const val SUCCESS_RESULT_CODE = 0
    }
}

private data class NhnResponseHeader(
    val resultCode: Int? = null,
    val resultMessage: String? = null,
    val isSuccessful: Boolean? = null,
)

private data class NhnResponseMessage(
    val requestId: String? = null,
    val sendResults: List<NhnRecipientResult>? = null,
)

private data class NhnRecipientResult(
    val recipientSeq: Int? = null,
    val recipientNo: String? = null,
    val resultCode: Int? = null,
    val resultMessage: String? = null,
)
