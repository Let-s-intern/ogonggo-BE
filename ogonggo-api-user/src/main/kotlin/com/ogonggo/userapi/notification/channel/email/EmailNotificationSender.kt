package com.ogonggo.userapi.notification.channel.email

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.mail.OgonggoMailProperties
import com.ogonggo.userapi.notification.delivery.implement.NotificationSender
import jakarta.mail.MessagingException
import jakarta.mail.SendFailedException
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * EMAIL 알림을 SMTP(SES)로 보내는 공통 발송 adapter. 개별 알림의 생성 조건·예정 시각·템플릿 값은 알지 않는다.
 *
 * SMTP 예외 메시지에는 수신 주소가 섞일 수 있어 result_detail에 원문을 남기지 않는다.
 */
@Component
internal class EmailNotificationSender(
    private val javaMailSender: JavaMailSender,
    private val properties: OgonggoMailProperties,
    private val objectMapper: ObjectMapper,
) : NotificationSender {
    override val channel = NotificationChannel.EMAIL

    override fun send(notification: NotificationMessageDto): NotificationDeliveryResult {
        if (properties.from.isBlank()) {
            return NotificationDeliveryResult.Failed(
                resultCode = "MAIL_FROM_NOT_CONFIGURED",
                failureCategory = NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
            )
        }
        val template = EmailTemplate.findByTemplateCode(notification.templateCode)
            ?: return NotificationDeliveryResult.Failed(
                resultCode = "UNKNOWN_EMAIL_TEMPLATE",
                failureCategory = NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            )
        val parameters = try {
            objectMapper.readValue(notification.payloadJson, STRING_MAP_TYPE)
        } catch (_: Exception) {
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_EMAIL_PAYLOAD",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
                detail = "메일 payload를 해석할 수 없습니다.",
            )
        }
        val message = try {
            createMessage(notification.recipientAddress, template.render(parameters))
        } catch (_: MessagingException) {
            // 수신 주소 형식 오류가 여기서 걸린다. 보내 봐야 같은 결과이므로 명시적 실패로 둔다.
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_EMAIL_MESSAGE",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
            )
        }

        return try {
            javaMailSender.send(message)
            NotificationDeliveryResult.Sent(message.messageID)
        } catch (_: MailAuthenticationException) {
            NotificationDeliveryResult.Failed(
                resultCode = "SMTP_AUTHENTICATION_FAILED",
                failureCategory = NotificationFailureCategory.AUTHENTICATION_CONFIGURATION,
            )
        } catch (exception: MailSendException) {
            exception.toDeliveryResult()
        } catch (_: MailException) {
            NotificationDeliveryResult.Unknown(
                resultCode = "SMTP_ERROR",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
            )
        }
    }

    private fun createMessage(recipientAddress: String, content: EmailContent): MimeMessage {
        val message = javaMailSender.createMimeMessage()
        // 텍스트와 HTML 본문을 함께 담으려면 multipart여야 한다.
        val helper = MimeMessageHelper(message, true, ENCODING)
        helper.setFrom(properties.from)
        helper.setTo(recipientAddress)
        helper.setSubject(content.subject)
        helper.setText(content.text, content.html)
        return message
    }

    /**
     * SMTP 서버가 수신자를 거절하면 SendFailedException이 남는다. 서버가 명시적으로 거절한 것이므로 FAILED로 둔다.
     * 그 밖의 발송 실패는 연결 도중 끊긴 경우를 포함해 접수 여부를 알 수 없으므로 UNKNOWN으로 둔다.
     */
    private fun MailSendException.toDeliveryResult(): NotificationDeliveryResult =
        if (messageExceptions.any { it is SendFailedException }) {
            NotificationDeliveryResult.Failed(
                resultCode = "SMTP_REJECTED",
                failureCategory = NotificationFailureCategory.CLIENT_ERROR,
            )
        } else {
            NotificationDeliveryResult.Unknown(
                resultCode = "SMTP_SEND_FAILED",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
            )
        }

    private companion object {
        const val ENCODING = "UTF-8"
        val STRING_MAP_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
