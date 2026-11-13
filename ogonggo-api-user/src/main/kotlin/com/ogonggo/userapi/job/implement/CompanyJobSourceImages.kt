package com.ogonggo.userapi.job.implement

/**
 * 기업회원이 올린 원문 이미지와 공고 원문 HTML(`job_sources`)을 서로 바꾼다.
 *
 * 기업회원 공고는 글 칸을 모두 쓰고, 원문 이미지는 선택으로 더한다. 원문 HTML은 크롤러 공고와 같은 자리에 두어
 * 공고 상세의 `공고 원문` 탭이 그대로 그린다. 이미지마다 문단 하나로 감싼다.
 *
 * 주소는 요청 검증에서 URL 형식을 거쳤지만 속성 값으로 넣으므로 HTML 특수문자를 이스케이프한다.
 * 다시 읽을 때는 `img` 의 `src` 를 순서대로 꺼낸다. 운영자가 어드민에서 원문을 고쳤어도 jsoup이 거른 HTML이라
 * `src="..."` 모양은 같다.
 */
internal object CompanyJobSourceImages {

    private val IMG_SRC = Regex("""<img\b[^>]*?\bsrc="([^"]*)"""", RegexOption.IGNORE_CASE)

    fun toHtml(urls: List<String>): String = urls.joinToString(separator = "") { url ->
        """<p><img src="${escape(url)}" alt="공고 원문 이미지"></p>"""
    }

    fun fromHtml(html: String?): List<String> {
        if (html.isNullOrBlank()) return emptyList()
        return IMG_SRC.findAll(html).map { unescape(it.groupValues[1]) }.toList()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "&#39;")

    private fun unescape(value: String): String = value
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
}
