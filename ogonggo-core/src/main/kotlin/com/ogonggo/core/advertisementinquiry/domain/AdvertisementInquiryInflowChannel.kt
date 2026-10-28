package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField
import java.net.URI
import java.net.URISyntaxException

/**
 * 문의가 어디서 들어왔는지를 저장할 때 한 번 판정한 값이다. 어드민 필터와 슬랙 알림이 같은 값을 쓴다.
 *
 * 판정이 틀렸을 때 확인할 수 있도록 판정에 쓴 원본(UTM, referrer)도 문의에 함께 남긴다.
 * [UTM_OTHER]는 정해 두지 않은 `utm_source`이고, 화면에는 그 원문을 보인다.
 * [EXTERNAL_SITE]는 화면에 referrer 호스트를 함께 보인다.
 */
enum class AdvertisementInquiryInflowChannel(
    override val code: Int,
    override val desc: String,
) : EnumField {
    COLDMAIL(1, "콜드메일"),
    INSTAGRAM(2, "인스타그램"),
    KAKAO(3, "카카오톡"),
    NEWSLETTER(4, "뉴스레터"),
    UTM_OTHER(5, "기타 UTM"),
    OGONGGO(6, "오공고 사이트"),
    LETSCAREER(7, "렛츠커리어"),
    SEARCH(8, "검색"),
    EXTERNAL_SITE(9, "외부 사이트"),
    DIRECT(10, "직접 접속"),
    ;

    companion object {
        private val UTM_SOURCES = mapOf(
            "coldmail" to COLDMAIL,
            "instagram" to INSTAGRAM,
            "kakao" to KAKAO,
            "newsletter" to NEWSLETTER,
        )
        private const val OGONGGO_DOMAIN = "ogonggo.co.kr"
        private const val LETSCAREER_DOMAIN = "letscareer.co.kr"
        private val SEARCH_ENGINE_LABELS = setOf("google", "naver", "daum", "bing")

        /**
         * 위에서부터 처음 맞는 규칙으로 판정한다.
         *
         * 1. `utmSource`가 있으면 정해 둔 이름, 모르는 값은 [UTM_OTHER]
         * 2. referrer 호스트가 오공고 → [OGONGGO]
         * 3. referrer 호스트가 렛츠커리어 → [LETSCAREER]
         * 4. referrer 호스트가 검색엔진(google, naver, daum, bing) → [SEARCH]
         * 5. referrer가 있으면 → [EXTERNAL_SITE]
         * 6. 그 밖 → [DIRECT]
         *
         * 콜드메일은 메일 앱이 Referer를 지우는 경우가 많아 UTM을 referrer보다 먼저 본다.
         * 대소문자를 가리지 않고, 호스트는 하위 도메인(`www.` 등)까지 같은 사이트로 본다.
         */
        fun classify(utmSource: String?, referrer: String?): AdvertisementInquiryInflowChannel {
            val source = utmSource?.trim()?.lowercase()
            if (!source.isNullOrEmpty()) {
                return UTM_SOURCES[source] ?: UTM_OTHER
            }
            if (referrer.isNullOrBlank()) {
                return DIRECT
            }
            val host = referrerHost(referrer) ?: return EXTERNAL_SITE
            return when {
                host.isSiteOf(OGONGGO_DOMAIN) -> OGONGGO
                host.isSiteOf(LETSCAREER_DOMAIN) -> LETSCAREER
                host.split('.').any { it in SEARCH_ENGINE_LABELS } -> SEARCH
                else -> EXTERNAL_SITE
            }
        }

        /** referrer의 호스트를 소문자로 준다. 비었거나 URL로 읽을 수 없으면 null이다. */
        fun referrerHost(referrer: String?): String? {
            if (referrer.isNullOrBlank()) {
                return null
            }
            return try {
                URI(referrer.trim()).host?.lowercase()?.takeIf { it.isNotEmpty() }
            } catch (exception: URISyntaxException) {
                null
            }
        }

        private fun String.isSiteOf(domain: String): Boolean = this == domain || endsWith(".$domain")
    }
}
