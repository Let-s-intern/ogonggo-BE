package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisementcontract.toSafeLogThrowable
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractMailSender
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractSlackNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AdvertisementContractService internal constructor(
    private val userReader: UserReader,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val advertisementContractNotifier: AdvertisementContractSlackNotifier,
    private val advertisementContractMailer: AdvertisementContractMailSender,
) {

    fun createContract(userId: Long?, command: CreateAdvertisementContractCommand): Long {
        // 공개 신청은 유효한 토큰에 회원 행이 없더라도 비회원 신청으로 접수한다.
        val registeredUserId = userId?.takeIf { userReader.readRoles(setOf(it)).containsKey(it) }

        val contractId = advertisementContractAppender.appendFromForm(
            AdvertisementContractFormAppendDto(
                userId = registeredUserId,
                promotionTarget = command.promotionTarget,
                companyName = command.companyName,
                recruitmentTitle = command.recruitmentTitle,
                recruitmentUrl = command.recruitmentUrl,
                recruitmentDeadline = command.recruitmentDeadline,
                purpose = command.purpose,
                categories = command.categories,
                scheduleRequest = command.scheduleRequest,
                advertisementRequest = command.advertisementRequest,
                managerName = command.managerName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                invoiceMethod = command.invoiceMethod,
                creativeMethod = command.creativeMethod,
            ),
        )

        val notification = AdvertisementContractNotification.from(contractId, command)
        try {
            advertisementContractNotifier.notify(notification)
        } catch (exception: RuntimeException) {
            log.error(
                "광고 계약 신청 Slack 알림에 실패했습니다. 신청은 저장돼 있습니다. contractId={}",
                contractId,
                exception.toSafeLogThrowable(),
            )
        }
        try {
            advertisementContractMailer.sendConfirmation(notification)
        } catch (exception: RuntimeException) {
            // @Async executor 제출 거절도 접수 결과를 바꾸지 않는다.
            log.error(
                "광고 계약 신청 확인 메일 예약에 실패했습니다. 신청은 저장돼 있습니다. contractId={}",
                contractId,
                exception.toSafeLogThrowable(),
            )
        }

        return contractId
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdvertisementContractService::class.java)
    }
}

/** 알림 어댑터에 전달하는 값이다. 자유 입력 요청사항은 외부 채널로 전달하지 않는다. */
data class AdvertisementContractNotification(
    val contractId: Long,
    val promotionTarget: String,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: String,
    val purpose: String,
    val categories: List<String>,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
) {
    companion object {
        fun from(contractId: Long, command: CreateAdvertisementContractCommand): AdvertisementContractNotification =
            AdvertisementContractNotification(
                contractId = contractId,
                promotionTarget = command.promotionTarget.desc,
                companyName = command.companyName,
                recruitmentTitle = command.recruitmentTitle,
                recruitmentDeadline = command.recruitmentDeadline.toString(),
                purpose = command.purpose.desc,
                categories = command.categories.sortedBy { it.code }.map { it.desc },
                managerName = command.managerName,
                email = command.email,
                phoneNumber = command.phoneNumber,
            )
    }
}
