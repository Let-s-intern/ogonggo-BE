package com.ogonggo.core.editor.lexical

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

private fun invalid(reason: String): Nothing = throw LexicalEditorStateException(reason)

/**
 * Lexical EditorState를 저장하기 전에 JSON 형식과 크기만 검증한다.
 * 검증을 통과한 JSON은 DB에 저장할 수 있는 canonical 문자열로 반환한다.
 */
@Component
class LexicalEditorStateValidator(
    private val objectMapper: ObjectMapper,
) {

    fun validateAndSerialize(content: String): String {
        if (content.length > MAX_SERIALIZED_LENGTH) {
            invalid("본문 JSON은 ${MAX_SERIALIZED_LENGTH}자 이하여야 합니다.")
        }
        if (content.isBlank()) {
            invalid("에디터 내용 JSON 형식이 올바르지 않습니다.")
        }

        val document = try {
            objectMapper.readTree(content)
        } catch (_: JsonProcessingException) {
            invalid("에디터 내용 JSON 형식이 올바르지 않습니다.")
        }

        if (document == null) {
            invalid("에디터 내용 JSON 형식이 올바르지 않습니다.")
        }

        return objectMapper.writeValueAsString(document)
    }

    private companion object {
        const val MAX_SERIALIZED_LENGTH = 200_000
    }
}
