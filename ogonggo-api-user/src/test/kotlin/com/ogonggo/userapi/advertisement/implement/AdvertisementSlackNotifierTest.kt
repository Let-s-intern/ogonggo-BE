package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.userapi.slack.SlackProperties
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

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
            // blocks가 있으면 슬랙이 text를 본문에 그리지 않아 첫 블록에 같은 줄을 넣는다.
            .andExpect(jsonPath("$.blocks[0].text.text").value("*📢 B2B 광고 문의 - 렛츠커리어 (지금 바로 무료 홍보 할래요)*"))
            .andExpect(jsonPath("$.blocks[1].fields[0].text").value("*기업명*\n렛츠커리어"))
            .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*이메일*\nmanager@ogonggo.co.kr"))
            .andExpect(jsonPath("$.blocks[1].fields[4].text").value("*신청 유형*\n지금 바로 무료 홍보 할래요"))
            .andExpect(
                jsonPath("$.blocks[1].fields[5].text").value("*희망 홍보 채널*\n오픈채팅방 · 마케팅"),
            )
            .andExpect(jsonPath("$.blocks[2].text.text").value("*현재 홍보 채널 · 어려운 점*\n$PROMOTION_ANSWER"))
            // 어드민에 문의 상세 화면이 없어 목록으로 보낸다.
            .andExpect(
                jsonPath("$.blocks[4].text.text").value("<https://admin.ogonggo.co.kr/business/ad-inquiries|어드민에서 보기>"),
            )
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(NOTIFICATION)

        server.verify()
    }

    @Test
    fun `회원 구분과 유입 경로를 UTM 값과 함께 한 줄로 싣는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(
                jsonPath("$.blocks[3].text.text")
                    .value("*회원 구분 · 유입 경로*\n기업 회원 · 콜드메일 · utm_campaign=free_promo · utm_content=templateA"),
            )
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(NOTIFICATION)

        server.verify()
    }

    @Test
    fun `정해 두지 않은 utm_source는 원문을, 외부 사이트는 referrer 호스트를 보인다`() {
        listOf(
            NOTIFICATION.copy(
                memberType = AdvertisementInquiryMemberType.GUEST,
                inflowChannel = AdvertisementInquiryInflowChannel.UTM_OTHER,
                utmSource = "linkedin",
                utmCampaign = null,
                utmContent = null,
            ) to "*회원 구분 · 유입 경로*\n비회원 · linkedin",
            NOTIFICATION.copy(
                memberType = AdvertisementInquiryMemberType.USER,
                inflowChannel = AdvertisementInquiryInflowChannel.EXTERNAL_SITE,
                utmSource = null,
                utmCampaign = null,
                utmContent = null,
                referrer = "https://blog.example.com/post/1",
            ) to "*회원 구분 · 유입 경로*\n일반 회원 · 외부 사이트 · blog.example.com",
            NOTIFICATION.copy(
                memberType = AdvertisementInquiryMemberType.GUEST,
                inflowChannel = AdvertisementInquiryInflowChannel.DIRECT,
                utmSource = null,
                utmCampaign = null,
                utmContent = null,
            ) to "*회원 구분 · 유입 경로*\n비회원 · 직접 접속",
        ).forEach { (notification, expected) ->
            val builder = RestClient.builder()
            val server = MockRestServiceServer.bindTo(builder).build()
            val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))
            server.expect(requestTo(WEBHOOK_URL))
                .andExpect(jsonPath("$.blocks[3].text.text").value(expected))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

            notifier.notify(notification)

            server.verify()
        }
    }

    /**
     * 슬랙은 section 블록에 쓰지 않는 필드가 null 로 들어 있으면 400 invalid_blocks 로 거절한다.
     * 직렬화 설정이 빠지면 payload 모양 검증만으로는 잡히지 않아 별도로 고정한다.
     */
    @Test
    fun `쓰지 않는 블록 필드는 null 로 보내지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            // 필드 블록에는 text 키가, 본문 블록에는 fields 키가 아예 없어야 한다.
            .andExpect(jsonPath("$.blocks[1].text").doesNotExist())
            .andExpect(jsonPath("$.blocks[2].fields").doesNotExist())
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
            .andExpect(jsonPath("$.blocks[2].text.text").value("*현재 홍보 채널 · 어려운 점*\n&lt;!channel&gt; A&amp;B"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(NOTIFICATION.copy(promotionAnswer = "<!channel> A&B"))

        server.verify()
    }

    @Test
    fun `웹훅이 실패로 응답하면 예외를 던진다`() {
        // 접수를 실패로 만들지는 서비스가 정한다. 여기서 삼키면 서비스가 실패를 로그로 남길 수 없다.
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError())

        assertThrows(RestClientException::class.java) { notifier.notify(NOTIFICATION) }
    }

    @Test
    fun `웹훅 주소가 비어 있으면 보내지 않고 예외를 던진다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementSlackNotifier(builder.build(), properties(""))

        assertThrows(IllegalStateException::class.java) { notifier.notify(NOTIFICATION) }
        server.verify()
    }

    private fun properties(advertisementInquiry: String): SlackProperties =
        SlackProperties(advertisementInquiry = advertisementInquiry)

    companion object {
        private const val WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/xxxx"
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있습니다."
        private val NOTIFICATION = AdvertisementInquiryNotification(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionChannel = AdvertisementInquiryPromotionChannel.OPEN_CHAT_MARKETING,
            promotionAnswer = PROMOTION_ANSWER,
            memberType = AdvertisementInquiryMemberType.COMPANY,
            inflowChannel = AdvertisementInquiryInflowChannel.COLDMAIL,
            utmSource = "coldmail",
            utmCampaign = "free_promo",
            utmContent = "templateA",
            referrer = null,
        )
    }
}
