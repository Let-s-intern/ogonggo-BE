package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel.Companion.classify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AdvertisementInquiryInflowChannelTest {

    @Test
    fun `1 - utm_source가 있으면 referrer보다 먼저 보고 정해 둔 이름으로 판정한다`() {
        assertEquals(AdvertisementInquiryInflowChannel.COLDMAIL, classify("coldmail", "https://www.google.com/"))
        assertEquals(AdvertisementInquiryInflowChannel.INSTAGRAM, classify("Instagram", null))
        assertEquals(AdvertisementInquiryInflowChannel.KAKAO, classify(" KAKAO ", null))
        assertEquals(AdvertisementInquiryInflowChannel.NEWSLETTER, classify("newsletter", null))
    }

    @Test
    fun `1 - 정해 두지 않은 utm_source는 기타 UTM이다`() {
        assertEquals(AdvertisementInquiryInflowChannel.UTM_OTHER, classify("linkedin", "https://www.ogonggo.co.kr/"))
    }

    @Test
    fun `2 - referrer 호스트가 오공고면 오공고 사이트다`() {
        assertEquals(AdvertisementInquiryInflowChannel.OGONGGO, classify(null, "https://www.ogonggo.co.kr/jobs/1"))
        assertEquals(AdvertisementInquiryInflowChannel.OGONGGO, classify(null, "https://OGONGGO.CO.KR/"))
        assertEquals(AdvertisementInquiryInflowChannel.OGONGGO, classify("", "https://ogonggo.co.kr"))
    }

    @Test
    fun `3 - referrer 호스트가 렛츠커리어면 렛츠커리어다`() {
        assertEquals(AdvertisementInquiryInflowChannel.LETSCAREER, classify(null, "https://www.letscareer.co.kr/program"))
        assertEquals(AdvertisementInquiryInflowChannel.LETSCAREER, classify(null, "https://letscareer.co.kr/"))
    }

    @Test
    fun `4 - referrer 호스트가 검색엔진이면 검색이다`() {
        listOf(
            "https://www.google.com/",
            "https://www.google.co.kr/search?q=오공고",
            "https://search.naver.com/search.naver?query=오공고",
            "https://m.search.daum.net/search",
            "https://www.Bing.com/",
        ).forEach { referrer ->
            assertEquals(AdvertisementInquiryInflowChannel.SEARCH, classify(null, referrer), referrer)
        }
    }

    @Test
    fun `5 - 그 밖의 referrer는 외부 사이트다`() {
        assertEquals(AdvertisementInquiryInflowChannel.EXTERNAL_SITE, classify(null, "https://blog.example.com/post"))
        // 이름이 비슷해도 다른 도메인은 오공고로 보지 않는다.
        assertEquals(AdvertisementInquiryInflowChannel.EXTERNAL_SITE, classify(null, "https://notogonggo.co.kr/"))
        assertEquals(AdvertisementInquiryInflowChannel.EXTERNAL_SITE, classify(null, "https://ogonggo.co.kr.evil.com/"))
        // 호스트를 읽을 수 없어도 referrer가 있으면 외부 사이트다.
        assertEquals(AdvertisementInquiryInflowChannel.EXTERNAL_SITE, classify(null, "not a url"))
    }

    @Test
    fun `6 - utm_source와 referrer가 없거나 비었으면 직접 접속이다`() {
        assertEquals(AdvertisementInquiryInflowChannel.DIRECT, classify(null, null))
        assertEquals(AdvertisementInquiryInflowChannel.DIRECT, classify("", ""))
        assertEquals(AdvertisementInquiryInflowChannel.DIRECT, classify("  ", "  "))
    }

    @Test
    fun `referrer 호스트는 소문자로 꺼내고 읽을 수 없으면 null이다`() {
        assertEquals("www.example.com", AdvertisementInquiryInflowChannel.referrerHost("https://WWW.Example.com/a?b=c"))
        assertNull(AdvertisementInquiryInflowChannel.referrerHost(""))
        assertNull(AdvertisementInquiryInflowChannel.referrerHost("not a url"))
    }
}
