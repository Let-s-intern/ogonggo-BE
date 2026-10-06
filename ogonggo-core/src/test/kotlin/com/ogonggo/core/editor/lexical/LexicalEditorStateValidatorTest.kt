package com.ogonggo.core.editor.lexical

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LexicalEditorStateValidatorTest {

    private val objectMapper = ObjectMapper()
    private val validator = LexicalEditorStateValidator(objectMapper)

    /** 검증 실패 시 호출자가 정한 예외를 그대로 던지는지 확인하려고 쓰는 예외다. */
    private class Rejected(reason: String) : RuntimeException(reason)

    private fun validate(content: String): String = validator.validateAndSerialize(content) { reason -> throw Rejected(reason) }

    @ParameterizedTest
    @ValueSource(strings = ["{}", "[]", "null", "\"plain text\""])
    fun `유효한 JSON을 저장 가능한 문자열로 변환한다`(content: String) {
        val result = validate(content)

        assertEquals(objectMapper.readTree(content), objectMapper.readTree(result))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "not-json", "{\"root\":}"])
    fun `JSON이 아니면 거부한다`(content: String) {
        assertThrows(Rejected::class.java) {
            validate(content)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [200_001, 250_000])
    fun `JSON 문자열이 최대 크기를 초과하면 거부한다`(contentLength: Int) {
        val content = "\"${"a".repeat(contentLength - 2)}\""

        assertThrows(Rejected::class.java) {
            validate(content)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 200_000])
    fun `JSON 문자열이 최대 크기 이하면 허용한다`(contentLength: Int) {
        val content = "\"${"a".repeat(contentLength - 2)}\""

        validate(content)
    }
}
