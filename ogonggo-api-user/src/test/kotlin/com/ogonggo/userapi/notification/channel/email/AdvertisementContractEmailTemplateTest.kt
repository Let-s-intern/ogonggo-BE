package com.ogonggo.userapi.notification.channel.email

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AdvertisementContractEmailTemplateTest {

    @Test
    @DisplayName("광고 신청 메일 템플릿이 주요 신청 필드를 텍스트와 HTML에 채운다")
    fun `광고 계약 메일에 공고명 홍보 대상 목적과 마감일이 모두 들어간다`() {
        // given
        val parameters = mapOf(
            "managerName" to "김담당",
            "companyName" to "샘플기업",
            "recruitmentTitle" to "백엔드 인턴 채용",
            "promotionTarget" to "채용 공고",
            "purpose" to "적합한 지원자 확보",
            "categories" to "배너 · 인스타그램 1",
            "recruitmentDeadline" to "2026-12-31",
        )

        // when
        val content = EmailTemplate.ADVERTISEMENT_CONTRACT_RECEIVED.render(parameters)

        // then
        assertEquals("[오공고] 광고 계약 신청이 접수되었습니다", content.subject)
        listOf("백엔드 인턴 채용", "채용 공고", "적합한 지원자 확보", "2026-12-31", "배너 · 인스타그램 1")
            .forEach { value ->
                assertTrue(content.text.contains(value), "텍스트 본문에 '$value'가 있어야 합니다.")
                assertTrue(content.html.contains(value), "HTML 본문에 '$value'가 있어야 합니다.")
            }
        assertTrue(content.html.contains("<li>공고명:"))
    }

    @Test
    @DisplayName("HTML 템플릿 변수는 문자 그대로 표시되도록 이스케이프한다")
    fun `회사명 HTML 제어 문자를 이스케이프한다`() {
        // given
        val parameters = mapOf("companyName" to "<샘플 & 회사>")

        // when
        val content = EmailTemplate.ADVERTISEMENT_CONTRACT_RECEIVED.render(parameters)

        // then
        assertTrue(content.html.contains("&lt;샘플 &amp; 회사&gt;"))
        assertTrue(content.text.contains("<샘플 & 회사>"))
    }
}
