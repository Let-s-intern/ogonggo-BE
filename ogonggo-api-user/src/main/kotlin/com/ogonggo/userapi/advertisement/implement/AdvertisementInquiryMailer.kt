package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.ADVERTISEMENT_TASK_EXECUTOR
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

interface AdvertisementInquiryMailer {
    /** 문의를 남긴 담당자에게 접수 확인 메일을 보낸다. 실패해도 접수에는 영향을 주지 않는다. */
    fun sendConfirmation(notification: AdvertisementInquiryNotification)
}

/**
 * 접수 확인 메일 발송.
 *
 * 슬랙 알림과 달리 실패를 삼킨다. 문의 자체는 이미 슬랙에 전달된 뒤라 메일이 빠져도 유실이 아니고,
 * 확인 메일 때문에 접수를 실패로 만들면 담당자가 폼을 다시 제출하게 된다.
 *
 * SMTP는 응답이 늦어질 수 있어 요청 스레드에서 보내지 않는다.
 * 지표 실행기는 스레드가 하나뿐이고 큐가 차면 버리도록 되어 있어 함께 쓰지 않는다.
 */
@Component
internal class AdvertisementInquiryMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: AdvertisementMailProperties,
) : AdvertisementInquiryMailer {

    @Async(ADVERTISEMENT_TASK_EXECUTOR)
    override fun sendConfirmation(notification: AdvertisementInquiryNotification) {
        if (properties.from.isBlank()) {
            log.warn("광고 문의 발신 주소가 설정되지 않아 접수 확인 메일을 건너뜁니다. companyName={}", notification.companyName)
            return
        }

        try {
            javaMailSender.send(createMessage(notification))
        } catch (exception: MailException) {
            log.warn("광고 문의 접수 확인 메일 발송에 실패했습니다. companyName={}", notification.companyName, exception)
        }
    }

    private fun createMessage(notification: AdvertisementInquiryNotification): SimpleMailMessage =
        SimpleMailMessage().apply {
            setFrom(properties.from)
            setTo(notification.email)
            setSubject(SUBJECT)
            setText(createBody(notification))
        }

    /**
     * 담당자가 무엇을 신청했는지 그대로 되돌려준다.
     * 문의 내용은 다시 싣지 않는다. 메일함에 남는 사본이라 길어질수록 읽히지 않는다.
     */
    private fun createBody(notification: AdvertisementInquiryNotification): String = buildString {
        append("안녕하세요, ").append(notification.managerName).append("님.\n")
        append("오공고에 광고 문의를 남겨주셔서 감사합니다.\n\n")
        append("아래 내용으로 접수되었습니다.\n")
        append("  기업명: ").append(notification.companyName).append('\n')
        append("  신청 유형: ").append(notification.inquiryType.desc).append('\n')
        append("  연락처: ").append(notification.phoneNumber).append("\n\n")
        append("담당자가 확인한 뒤 이 주소로 회신드리겠습니다.\n\n")
        append("감사합니다.\n")
        append("오공고 드림\n")
    }

    companion object {
        private const val SUBJECT = "[오공고] 광고 문의가 접수되었습니다"
        private val log = LoggerFactory.getLogger(AdvertisementInquiryMailSender::class.java)
    }
}
