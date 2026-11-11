package com.ogonggo.core.user.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompanyTemporaryPasswordGeneratorTest {

    @Test
    fun `임시 비밀번호는 12자이고 영문과 숫자를 섞으며 헷갈리는 글자를 쓰지 않는다`() {
        val generator = CompanyTemporaryPasswordGenerator()

        repeat(200) {
            val password = generator.generate()

            assertEquals(12, password.length)
            assertTrue(password.any(Char::isLetter) && password.any(Char::isDigit), password)
            assertTrue(password.none { it in "0O1lI" }, password)
        }
    }
}
