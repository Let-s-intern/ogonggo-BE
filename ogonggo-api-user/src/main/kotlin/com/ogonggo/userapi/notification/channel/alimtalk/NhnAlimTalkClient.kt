package com.ogonggo.userapi.notification.channel.alimtalk

import com.ogonggo.userapi.config.UserAlimTalkConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

private const val NHN_DUPLICATE_IDEMPOTENCY_KEY_RESULT_CODE = -1005

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
            // 응답이 불명확해도 자동 재시도하지 않고 최종 실패로 기록한다.
            val safeException = RuntimeException("NHN 알림톡 요청 중 통신 오류가 발생했습니다.").apply {
                stackTrace = exception.stackTrace
            }
            log.error(
                "NHN 알림톡 응답을 받지 못했습니다. templateCode={}, errorType={}",
                message.templateCode,
                exception::class.simpleName,
                safeException,
            )
            NhnAlimTalkResult.Rejected("NHN_TRANSPORT_ERROR")
        } catch (exception: RestClientResponseException) {
            // HTTP 오류도 자동 재시도하지 않고 결과 코드만 안전하게 보존한다.
            NhnAlimTalkResult.Rejected("NHN_HTTP_${exception.statusCode.value()}")
        } catch (exception: RestClientException) {
            // 응답 변환 등 분류할 계약이 없는 오류도 최종 실패로 보존한다.
            log.warn(
                "NHN 알림톡 응답을 분류하지 못했습니다. templateCode={}, errorType={}",
                message.templateCode,
                exception::class.simpleName,
            )
            NhnAlimTalkResult.Rejected("NHN_CLIENT_ERROR")
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
            NhnAlimTalkResult.DuplicateIdempotencyKey ->
                log.info(
                    "NHN 알림톡 중복 멱등성 키 응답을 받았습니다. templateCode={}, resultCode={}",
                    templateCode,
                    NHN_DUPLICATE_IDEMPOTENCY_KEY_RESULT_CODE,
                )
            is NhnAlimTalkResult.Rejected ->
                log.warn("NHN 알림톡 요청이 거절됐습니다. templateCode={}, resultCode={}", templateCode, result.resultCode)
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
            // NHN은 10분 내 같은 멱등성 키 요청을 -1005로 거절한다. 다른 오류는 일반 거절로 유지한다.
            if (header.resultCode == NHN_DUPLICATE_IDEMPOTENCY_KEY_RESULT_CODE) {
                return NhnAlimTalkResult.DuplicateIdempotencyKey
            }
            return NhnAlimTalkResult.Rejected(header.resultCode?.toString())
        }

        val resultCode = message?.sendResults?.singleOrNull()?.resultCode
            ?: return NhnAlimTalkResult.InvalidResponse
        return if (resultCode == SUCCESS_RESULT_CODE) {
            NhnAlimTalkResult.Accepted(message.requestId)
        } else {
            NhnAlimTalkResult.Rejected(resultCode.toString())
        }
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
