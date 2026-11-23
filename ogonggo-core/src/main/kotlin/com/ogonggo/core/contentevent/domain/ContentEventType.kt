package com.ogonggo.core.contentevent.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 브라우저가 보내는 콘텐츠 이벤트의 종류다.
 *
 * - 상세 화면 이벤트(조회·CTA 클릭·스크롤·공유)는 채용공고·부트캠프 하나를 가리켜야 한다.
 * - 광고 소재 이벤트(노출·클릭)는 광고 노출을 가리켜야 한다. 배너는 대상 공고가 없을 수 있다.
 */
enum class ContentEventType(
    override val code: Int,
    override val desc: String,
    val requiresTarget: Boolean,
    val requiresExposure: Boolean,
) : EnumField {
    VIEW(1, "상세 조회", requiresTarget = true, requiresExposure = false),
    CTA_CLICK(2, "CTA 클릭", requiresTarget = true, requiresExposure = false),
    SCROLL(3, "스크롤", requiresTarget = true, requiresExposure = false),
    IMPRESSION(4, "광고 노출", requiresTarget = false, requiresExposure = true),
    AD_CLICK(5, "광고 클릭", requiresTarget = false, requiresExposure = true),
    SHARE(6, "공유", requiresTarget = true, requiresExposure = false),
}
