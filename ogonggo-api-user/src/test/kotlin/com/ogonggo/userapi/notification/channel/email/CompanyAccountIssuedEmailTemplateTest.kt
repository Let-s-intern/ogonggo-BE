package com.ogonggo.userapi.notification.channel.email

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompanyAccountIssuedEmailTemplateTest {

    @Test
    fun `비즈니스 계정 발급 메일에 로그인 이메일과 임시 비밀번호가 텍스트와 HTML에 모두 들어간다`() {
        // given
        val parameters = mapOf(
            "managerName" to "김담당",
            "companyName" to "샘플기업",
            "email" to "ad@example.com",
            "temporaryPassword" to "Ab3dEf7hJk9m",
        )

        // when
        val content = EmailTemplate.COMPANY_ACCOUNT_ISSUED.render(parameters)

        // then
        assertEquals("[오공고] 비즈니스 계정이 발급되었습니다", content.subject)
        listOf("김담당", "샘플기업", "ad@example.com", "Ab3dEf7hJk9m").forEach { value ->
            assertTrue(content.text.contains(value), "텍스트 본문에 '$value'가 있어야 합니다.")
            assertTrue(content.html.contains(value), "HTML 본문에 '$value'가 있어야 합니다.")
        }
    }
}
