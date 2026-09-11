package com.ogonggo.core.editor.lexical

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LexicalEditorStateValidatorTest {

    private val objectMapper = ObjectMapper()
    private val validator = LexicalEditorStateValidator(objectMapper)

    @ParameterizedTest
    @ValueSource(strings = ["{}", "[]", "null", "\"plain text\""])
    fun `유효한 JSON을 저장 가능한 문자열로 변환한다`(content: String) {
        val result = validator.validateAndSerialize(content)

        assertEquals(objectMapper.readTree(content), objectMapper.readTree(result))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "not-json", "{\"root\":}"])
    fun `JSON이 아니면 거부한다`(content: String) {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(content)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [200_001, 250_000])
    fun `JSON 문자열이 최대 크기를 초과하면 거부한다`(contentLength: Int) {
        val content = "\"${"a".repeat(contentLength - 2)}\""

        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(content)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 200_000])
    fun `JSON 문자열이 최대 크기 이하면 허용한다`(contentLength: Int) {
        val content = "\"${"a".repeat(contentLength - 2)}\""

        validator.validateAndSerialize(content)
    }
}
