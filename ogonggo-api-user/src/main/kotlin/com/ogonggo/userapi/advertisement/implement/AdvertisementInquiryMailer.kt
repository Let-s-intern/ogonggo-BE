package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.ADVERTISEMENT_TASK_EXECUTOR
import com.ogonggo.userapi.mail.OgonggoMailProperties
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

interface AdvertisementInquiryMailer {
    /** 문의를 남긴 담당자에게 상품 소개서를 붙인 접수 확인 메일을 보낸다. 실패해도 접수에는 영향을 주지 않는다. */
    fun sendConfirmation(notification: AdvertisementInquiryNotification)
}

/**
 * 접수 확인 메일 발송.
 *
 * 실패를 삼킨다. 문의는 이미 저장된 뒤라 메일이 빠져도 유실이 아니고,
 * 확인 메일 때문에 접수를 실패로 만들면 담당자가 폼을 다시 제출하게 된다.
 *
 * SMTP는 응답이 늦어질 수 있어 요청 스레드에서 보내지 않는다.
 * 지표 실행기는 스레드가 하나뿐이고 큐가 차면 버리도록 되어 있어 함께 쓰지 않는다.
 *
 * 소개서를 붙여야 해서 SimpleMailMessage 대신 MimeMessage 를 쓴다. 전자는 평문 전용이라 첨부가 안 된다.
 */
@Component
internal class AdvertisementInquiryMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: OgonggoMailProperties,
) : AdvertisementInquiryMailer {

    @Async(ADVERTISEMENT_TASK_EXECUTOR)
    override fun sendConfirmation(notification: AdvertisementInquiryNotification) {
        if (properties.from.isBlank()) {
            log.warn("광고 문의 발신 주소가 설정되지 않아 접수 확인 메일을 건너뜁니다. companyName={}", notification.companyName)
            return
        }

        try {
            javaMailSender.send(createMessage(notification))
        } catch (exception: Exception) {
            // 첨부 파일을 읽지 못하는 경우까지 여기서 막는다. 어떤 이유로든 메일 때문에 접수가 깨지면 안 된다.
            log.warn("광고 문의 접수 확인 메일 발송에 실패했습니다. companyName={}", notification.companyName, exception)
        }
    }

    private fun createMessage(notification: AdvertisementInquiryNotification): MimeMessage {
        val message = javaMailSender.createMimeMessage()
        // multipart 를 켜야 본문과 첨부를 함께 담을 수 있다. 인코딩을 넘겨야 한글 파일명도 깨지지 않는다.
        val helper = MimeMessageHelper(message, true, ENCODING)
        helper.setFrom(properties.from)
        helper.setTo(notification.email)
        helper.setSubject(SUBJECT)
        helper.setText(createBody(notification), false)
        helper.addAttachment(ATTACHMENT_FILE_NAME, ClassPathResource(ATTACHMENT_PATH))
        return message
    }

    /**
     * 담당자가 무엇을 신청했는지 그대로 되돌려준다.
     * 문의 내용은 다시 싣지 않는다. 메일함에 남는 사본이라 길어질수록 읽히지 않는다.
     */
    private fun createBody(notification: AdvertisementInquiryNotification): String = buildString {
        append("안녕하세요, ").append(notification.managerName).append("님.\n")
        append("렛츠커리어 채용공고 광고에 문의해 주셔서 감사합니다.\n\n")
        append("아래 내용으로 접수되었습니다.\n")
        append("  기업명: ").append(notification.companyName).append('\n')
        append("  신청 유형: ").append(notification.inquiryType.desc).append('\n')
        append("  희망 홍보 채널: ").append(notification.promotionChannel.desc).append('\n')
        append("  연락처: ").append(notification.phoneNumber).append("\n\n")
        append(guideOf(notification.inquiryType)).append("\n\n")
        append("채용공고 홍보 상품 소개서를 첨부해 드립니다. 상품 구성과 집행 사례를 먼저 살펴봐 주세요.\n")
        append("담당자가 확인한 뒤 이 주소로 회신드리겠습니다.\n\n")
        append("감사합니다.\n")
        append("렛츠커리어 드림\n")
        append("official@letscareer.co.kr\n")
    }

    /** 소개서보다 먼저, 신청한 건이 어떻게 진행되는지 알린다. 무료 홍보 신청자가 유료 상품 안내만 받고 끝나지 않게 한다. */
    private fun guideOf(inquiryType: AdvertisementInquiryType): String = when (inquiryType) {
        AdvertisementInquiryType.FREE_PROMOTION ->
            "출시 기념 무료 홍보 신청이 접수되었습니다. 담당자가 신청 내용을 확인한 뒤 게시 일정과 필요한 자료를 안내드리겠습니다."
        AdvertisementInquiryType.LAUNCH_ALERT ->
            "출시 알림 신청이 접수되었습니다. 비즈 오공고가 열리면 이 주소로 가장 먼저 알려드리겠습니다."
    }

    companion object {
        private const val SUBJECT = "[렛츠커리어] 채용공고 광고 문의가 접수되었습니다"
        private const val ENCODING = "UTF-8"

        /** 클래스패스 이름은 아스키로 둔다. 한글 경로는 빌드·실행 환경에 따라 인코딩이 달라진다. */
        private const val ATTACHMENT_PATH = "mail/letscareer-job-ad-proposal.pdf"

        /** 받는 사람이 메일함에서 보는 이름 */
        private const val ATTACHMENT_FILE_NAME = "[렛츠커리어] 채용공고 광고 상품 소개서.pdf"

        private val log = LoggerFactory.getLogger(AdvertisementInquiryMailSender::class.java)
    }
}
