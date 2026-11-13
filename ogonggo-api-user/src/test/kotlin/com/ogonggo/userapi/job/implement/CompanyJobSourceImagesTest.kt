package com.ogonggo.userapi.job.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompanyJobSourceImagesTest {

    @Test
    fun `원문 HTML로 만든 이미지 주소를 같은 순서로 다시 읽는다`() {
        // given
        val urls = listOf("https://bucket.example.com/a.png?w=1&h=2", "https://bucket.example.com/b.png")

        // when
        val read = CompanyJobSourceImages.fromHtml(CompanyJobSourceImages.toHtml(urls))

        // then
        assertEquals(urls, read)
    }

    @Test
    fun `주소의 따옴표와 꺾쇠는 속성 밖으로 나가지 않게 이스케이프한다`() {
        val html = CompanyJobSourceImages.toHtml(listOf("https://example.com/a.png\"><script>"))

        assertEquals("""<p><img src="https://example.com/a.png&quot;&gt;&lt;script&gt;" alt="공고 원문 이미지"></p>""", html)
    }

    @Test
    fun `운영자가 거른 원문처럼 다른 속성이 섞여 있어도 이미지 주소만 읽는다`() {
        val html = """<h2>안내</h2><p><img alt="포스터" src="https://example.com/a.png"></p><p>글</p>"""

        assertEquals(listOf("https://example.com/a.png"), CompanyJobSourceImages.fromHtml(html))
    }

    @Test
    fun `원문이 없으면 빈 목록이다`() {
        assertEquals(emptyList<String>(), CompanyJobSourceImages.fromHtml(null))
    }
}
