package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisementcontract.toSafeLogThrowable
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractNotificationAppender
import com.ogonggo.userapi.advertisementcontract.implement.dto.AdvertisementContractNotificationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AdvertisementContractService internal constructor(
    private val userReader: UserReader,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val advertisementContractNotificationAppender: AdvertisementContractNotificationAppender,
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
                budgetAmount = command.budgetAmount,
                categories = command.categories,
                scheduleRequest = command.scheduleRequest,
                advertisementRequest = command.advertisementRequest,
                managerName = command.managerName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                businessNumberType = command.businessNumberType,
                businessNumber = command.businessNumber,
                invoiceMethod = command.invoiceMethod,
                creativeMethod = command.creativeMethod,
            ),
        )

        try {
            val inserted = advertisementContractNotificationAppender.append(
                AdvertisementContractNotificationDto.from(contractId, command),
            )
            log.info("광고 계약 신청 알림 적재 결과. contractId={}, inserted={}", contractId, inserted)
        } catch (exception: Exception) {
            log.error(
                "광고 계약 신청 알림을 적재하지 못했습니다. 신청은 저장돼 있습니다. contractId={}",
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
