package com.ogonggo.adminapi.job.implement

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * AI 서버가 보낸 원문 HTML에서 공고 상세에 그려도 안전한 태그와 속성만 남긴다.
 *
 * AI 서버도 정리해서 보내지만, 사용자 화면이 이 HTML을 그대로 그리므로 서버가 한 번 더 거른다.
 * 스크립트·스타일·이벤트 속성·`javascript:` 주소는 모두 빠진다. 상대 주소는 원문 URL 기준 절대 주소로 바꾸고,
 * 바꿀 수 없으면 뺀다. 글도 이미지도 남지 않으면 `null`이다.
 */
internal object JobSourceHtmlSanitizer {

    private val SAFELIST: Safelist = Safelist.relaxed()
        .addTags("hr")
        .removeAttributes("img", "align", "height", "width", "title")
        .removeAttributes("table", "summary", "width")
        .removeAttributes("td", "abbr", "axis", "width")
        .removeAttributes("th", "abbr", "axis", "width", "scope")
        .removeAttributes("col", "span", "width")
        .removeAttributes("colgroup", "span", "width")
        .removeAttributes("a", "title")
        .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer")
        .addEnforcedAttribute("a", "target", "_blank")

    private val OUTPUT = Document.OutputSettings().prettyPrint(false)

    fun sanitize(html: String?, sourceUrl: String?): String? {
        if (html.isNullOrBlank()) {
            return null
        }
        val cleaned = Jsoup.clean(html, sourceUrl.orEmpty(), SAFELIST, OUTPUT).trim()
        val body = Jsoup.parseBodyFragment(cleaned).body()
        return cleaned.takeIf { body.text().isNotBlank() || body.selectFirst("img") != null }
    }
}
