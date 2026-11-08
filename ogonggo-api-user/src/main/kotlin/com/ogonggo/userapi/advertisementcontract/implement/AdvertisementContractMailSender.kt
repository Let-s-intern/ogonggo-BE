package com.ogonggo.userapi.advertisementcontract.implement

import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractNotification
import com.ogonggo.userapi.advertisementcontract.toSafeLogThrowable
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.ADVERTISEMENT_CONTRACT_TASK_EXECUTOR
import com.ogonggo.userapi.mail.OgonggoMailProperties
import org.slf4j.LoggerFactory
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/** 접수 확인은 광고 계약 전용 executor와 발신 주소를 사용한다. */
@Component
internal class AdvertisementContractMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: OgonggoMailProperties,
) {

    @Async(ADVERTISEMENT_CONTRACT_TASK_EXECUTOR)
    fun sendConfirmation(notification: AdvertisementContractNotification) {
        if (properties.from.isBlank()) {
            log.warn("광고 계약 신청 발신 주소가 설정되지 않아 확인 메일을 건너뜁니다. contractId={}", notification.contractId)
            return
        }

        try {
            val message = SimpleMailMessage().apply {
                setFrom(properties.from)
                setTo(notification.email)
                subject = SUBJECT
                text = notification.confirmationBody()
            }
            javaMailSender.send(message)
        } catch (exception: Exception) {
            log.error(
                "광고 계약 신청 확인 메일 발송에 실패했습니다. contractId={}",
                notification.contractId,
                exception.toSafeLogThrowable(),
            )
        }
    }

    private fun AdvertisementContractNotification.confirmationBody(): String = buildString {
        append("안녕하세요, ").append(managerName).append("님.\n")
        append("오공고 광고 계약 신청이 접수되었습니다. 담당자가 확인한 뒤 연락드리겠습니다.\n\n")
        append("  기업명: ").append(companyName).append('\n')
        append("  공고명: ").append(recruitmentTitle).append('\n')
        append("  홍보 대상 · 목적: ").append(promotionTarget).append(" · ").append(purpose).append('\n')
        append("  신청 카테고리: ").append(categories.joinToString(" · ")).append('\n')
        append("  모집 마감일: ").append(recruitmentDeadline).append("\n\n")
        append("신청하신 카테고리는 희망 분야이며, 실제 상품과 일정은 담당자가 확인 후 안내드립니다.\n\n")
        append("감사합니다.\n오공고 드림\n")
    }

    companion object {
        private const val SUBJECT = "[오공고] 광고 계약 신청이 접수되었습니다"
        private val log = LoggerFactory.getLogger(AdvertisementContractMailSender::class.java)
    }
}
