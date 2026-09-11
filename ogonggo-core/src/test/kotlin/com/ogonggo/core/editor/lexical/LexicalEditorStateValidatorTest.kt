package com.ogonggo.core.editor.lexical

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LexicalEditorStateValidatorTest {

    private val objectMapper = ObjectMapper()
    private val validator = LexicalEditorStateValidator(objectMapper)

    @Test
    fun `유효한 Lexical EditorState JSON을 저장 가능한 문자열로 변환한다`() {
        val result = validator.validateAndSerialize(editorState(textNode("본문")))
        val document = objectMapper.readTree(result)

        assertEquals("root", document.at("/root/type").asText())
        assertEquals("본문", document.at("/root/children/0/text").asText())
    }

    @ParameterizedTest
    @ValueSource(strings = ["http", "https", "mailto"])
    fun `링크는 허용된 프로토콜만 받는다`(protocol: String) {
        val url = if (protocol == "mailto") "mailto:team@example.com" else "$protocol://example.com"

        validator.validateAndSerialize(
            editorState("""{"type":"link","url":"$url","children":[]}"""),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["javascript", "data", "ftp"])
    fun `링크의 위험한 프로토콜은 거부한다`(protocol: String) {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(
                editorState("""{"type":"link","url":"$protocol:alert(1)","children":[]}"""),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["http", "https"])
    fun `이미지는 http 또는 https URL만 받는다`(protocol: String) {
        validator.validateAndSerialize(
            editorState("""{"type":"image","src":"$protocol://cdn.example.com/image.webp"}"""),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["javascript", "data", "ftp"])
    fun `이미지의 위험한 프로토콜은 거부한다`(protocol: String) {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(
                editorState("""{"type":"image","src":"$protocol:alert(1)"}"""),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["script", "iframe", "unknown"])
    fun `지원하지 않는 Lexical 노드는 거부한다`(type: String) {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(editorState("""{"type":"$type","children":[]}"""))
        }
    }

    @Test
    fun `텍스트 길이가 제한을 초과하면 거부한다`() {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize(editorState(textNode("a".repeat(100_001))))
        }
    }

    @Test
    fun `JSON이 아니거나 root가 없으면 거부한다`() {
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize("[]")
        }
        assertThrows(LexicalEditorStateException::class.java) {
            validator.validateAndSerialize("{}")
        }
    }

    private fun editorState(vararg children: String): String =
        """{"root":{"children":[${children.joinToString(",")}],"type":"root","version":1}}"""

    private fun textNode(text: String): String =
        """{"detail":0,"format":0,"mode":"normal","style":"","text":"$text","type":"text","version":1}"""
}
