package com.ogonggo.userapi.advertisement.business

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.userapi.advertisement.error.AdvertisementErrorCode
import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryNotifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AdvertisementInquiryServiceTest {

    private val advertisementInquiryNotifier = Mockito.mock(AdvertisementInquiryNotifier::class.java)
    private val service = AdvertisementInquiryService(advertisementInquiryNotifier)

    @Test
    fun `문의 내용을 그대로 알림으로 전달한다`() {
        service.createInquiry(COMMAND)

        Mockito.verify(advertisementInquiryNotifier).notify(NOTIFICATION)
    }

    @Test
    fun `알림 발송에 실패하면 접수도 실패한다`() {
        // 저장소가 없어 알림이 곧 접수 결과다. 실패를 삼키면 사라진 문의를 접수됐다고 알리게 된다.
        Mockito.doThrow(InternalServerException(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED))
            .`when`(advertisementInquiryNotifier)
            .notify(NOTIFICATION)

        val exception = assertThrows(InternalServerException::class.java) { service.createInquiry(COMMAND) }

        assertEquals(
            AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED,
            exception.errorCode,
        )
    }

    companion object {
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있는데 인턴 공고는 지원자 편차가 큽니다."
        private val COMMAND = CreateAdvertisementInquiryCommand(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionAnswer = PROMOTION_ANSWER,
        )
        private val NOTIFICATION = AdvertisementInquiryNotification(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionAnswer = PROMOTION_ANSWER,
        )
    }
}
