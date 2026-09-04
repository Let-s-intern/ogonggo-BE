package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.error.AdvertisementErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class AdvertisementSlackNotifierTest {

    @Test
    fun `기업명과 신청 유형을 알림 제목에 담아 웹훅으로 보낸다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.text").value("📢 B2B 광고 문의 - 렛츠커리어 (지금 바로 무료 홍보 할래요)"))
            .andExpect(jsonPath("$.blocks[0].fields[0].text").value("*기업명*\n렛츠커리어"))
            .andExpect(jsonPath("$.blocks[0].fields[2].text").value("*이메일*\nmanager@ogonggo.co.kr"))
            .andExpect(jsonPath("$.blocks[0].fields[4].text").value("*신청 유형*\n지금 바로 무료 홍보 할래요"))
            .andExpect(jsonPath("$.blocks[1].text.text").value("*현재 홍보 채널 · 어려운 점*\n$PROMOTION_ANSWER"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(NOTIFICATION)

        server.verify()
    }

    @Test
    fun `문의자가 넣은 슬랙 제어 문자를 그대로 보내지 않는다`() {
        // 이스케이프하지 않으면 <http://...|링크>나 <!channel> 같은 문법으로 해석된다.
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks[1].text.text").value("*현재 홍보 채널 · 어려운 점*\n&lt;!channel&gt; A&amp;B"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(NOTIFICATION.copy(promotionAnswer = "<!channel> A&B"))

        server.verify()
    }

    @Test
    fun `웹훅이 실패로 응답하면 접수 실패로 바꾼다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError())

        val exception = assertThrows(InternalServerException::class.java) { notifier.notify(NOTIFICATION) }

        assertEquals(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED, exception.errorCode)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.errorCode.httpStatus)
    }

    @Test
    fun `웹훅 주소가 비어 있으면 보내지 않고 접수 실패로 응답한다`() {
        // 값이 없다고 조용히 넘어가면 문의가 사라진 채 성공 응답만 나간다.
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(""))

        val exception = assertThrows(InternalServerException::class.java) { notifier.notify(NOTIFICATION) }

        assertEquals(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED, exception.errorCode)
        server.verify()
    }

    private fun properties(inquiryUrl: String): AdvertisementSlackProperties =
        AdvertisementSlackProperties(inquiryUrl = inquiryUrl)

    companion object {
        private const val WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/xxxx"
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있습니다."
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
