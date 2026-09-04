package com.ogonggo.userapi.advertisement.business

import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * B2B 광고 문의 접수.
 *
 * 문의를 저장하지 않고 영업 슬랙 채널로만 전달한다. 저장하지 않기로 한 결정에 따라
 * @Transactional을 두지 않는다. DB를 쓰지 않는 흐름에 트랜잭션을 열면 커넥션만 점유한다.
 *
 * 알림을 비동기로 두지 않는다. 저장소가 없어 알림이 곧 접수 결과이므로,
 * 발송 실패를 성공 응답 뒤에 숨기면 접수됐다는 응답만 남고 문의는 사라진다.
 */
@Service
class AdvertisementInquiryService(
    private val advertisementInquiryNotifier: AdvertisementInquiryNotifier,
) {

    fun createInquiry(command: CreateAdvertisementInquiryCommand) {
        // 알림이 유일한 기록이라 접수 사실만이라도 서버에 남긴다.
        // 이메일과 전화번호는 여기 남기지 않는다. 발송에 실패한 경우에만 어댑터가 원문을 남긴다.
        log.info(
            "B2B 광고 문의를 접수했습니다. companyName={}, managerName={}, inquiryType={}",
            command.companyName,
            command.managerName,
            command.inquiryType,
        )

        advertisementInquiryNotifier.notify(AdvertisementInquiryNotification.from(command))
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdvertisementInquiryService::class.java)
    }
}

data class CreateAdvertisementInquiryCommand(
    val companyName: String,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val inquiryType: AdvertisementInquiryType,
    val promotionAnswer: String,
)

/** 알림 어댑터에 전달하는 값이다. Command와 필드가 같아도 변경 이유가 달라 따로 둔다. */
data class AdvertisementInquiryNotification(
    val companyName: String,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val inquiryType: AdvertisementInquiryType,
    val promotionAnswer: String,
) {
    companion object {
        fun from(command: CreateAdvertisementInquiryCommand): AdvertisementInquiryNotification =
            AdvertisementInquiryNotification(
                companyName = command.companyName,
                managerName = command.managerName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                inquiryType = command.inquiryType,
                promotionAnswer = command.promotionAnswer,
            )
    }
}
