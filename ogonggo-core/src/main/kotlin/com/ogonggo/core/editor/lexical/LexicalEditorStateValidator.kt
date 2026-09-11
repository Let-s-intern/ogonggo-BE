package com.ogonggo.core.editor.lexical

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.net.URI

private fun invalid(reason: String): Nothing = throw LexicalEditorStateException(reason)

/**
 * Lexical EditorState를 저장하기 전에 구조와 URL을 검증한다.
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

        val document = try {
            objectMapper.readTree(content)
        } catch (_: JsonProcessingException) {
            invalid("Lexical EditorState JSON 형식이 올바르지 않습니다.")
        }

        if (document == null || !document.isObject) {
            invalid("본문은 Lexical EditorState 객체여야 합니다.")
        }

        val root = document.get("root")
        if (root == null) {
            invalid("본문에 Lexical root 노드가 필요합니다.")
        }

        val state = ValidationState()
        validateNode(root, depth = 0, state = state, isRoot = true)
        if (state.textLength > MAX_TEXT_LENGTH) {
            invalid("본문 텍스트는 ${MAX_TEXT_LENGTH}자 이하여야 합니다.")
        }

        return objectMapper.writeValueAsString(document)
    }

    private fun validateNode(
        node: JsonNode,
        depth: Int,
        state: ValidationState,
        isRoot: Boolean = false,
    ) {
        if (!node.isObject) {
            invalid("본문 노드는 JSON 객체여야 합니다.")
        }
        if (depth > MAX_DEPTH) {
            invalid("본문의 중첩 깊이가 너무 깊습니다.")
        }
        state.nodeCount++
        if (state.nodeCount > MAX_NODE_COUNT) {
            invalid("본문의 노드 수가 너무 많습니다.")
        }

        val type = node.get("type")?.asText()
        if (type !in ALLOWED_NODE_TYPES) {
            invalid("지원하지 않는 Lexical 노드입니다: $type")
        }
        if (isRoot && type != ROOT_NODE_TYPE) {
            invalid("본문의 최상위 노드는 root여야 합니다.")
        }

        when (type) {
            "text" -> {
                val text = node.get("text")
                if (text == null || !text.isTextual) {
                    invalid("text 노드에는 문자열 text가 필요합니다.")
                }
                state.textLength += text.textValue().length
            }
            "link", "autolink" -> validateUrl(node, "url", image = false)
            "image" -> validateUrl(node, "src", image = true)
        }

        val children = node.get("children")
        if (type == ROOT_NODE_TYPE && (children == null || !children.isArray)) {
            invalid("root 노드에는 children 배열이 필요합니다.")
        }
        if (children != null && !children.isArray) {
            invalid("children은 배열이어야 합니다.")
        }
        children?.forEach { child -> validateNode(child, depth + 1, state) }
    }

    private fun validateUrl(node: JsonNode, field: String, image: Boolean) {
        val valueNode = node.get(field)
        if (valueNode == null || !valueNode.isTextual) {
            invalid("$field URL이 올바르지 않습니다.")
        }
        val value = valueNode.textValue()
        if (value.isBlank() || value.length > MAX_URL_LENGTH) {
            invalid("$field URL이 올바르지 않습니다.")
        }

        val scheme = try {
            URI(value).scheme?.lowercase()
        } catch (_: IllegalArgumentException) {
            null
        }
        val allowedSchemes = if (image) IMAGE_URL_SCHEMES else LINK_URL_SCHEMES
        if (scheme !in allowedSchemes) {
            invalid("$field URL 프로토콜을 사용할 수 없습니다.")
        }
    }

    private class ValidationState(
        var nodeCount: Int = 0,
        var textLength: Int = 0,
    )

    private companion object {
        const val ROOT_NODE_TYPE = "root"
        const val MAX_DEPTH = 32
        const val MAX_NODE_COUNT = 1_000
        const val MAX_TEXT_LENGTH = 100_000
        const val MAX_SERIALIZED_LENGTH = 200_000
        const val MAX_URL_LENGTH = 2_048
        val IMAGE_URL_SCHEMES = setOf("http", "https")
        val LINK_URL_SCHEMES = setOf("http", "https", "mailto")
        val ALLOWED_NODE_TYPES = setOf(
            "root",
            "paragraph",
            "heading",
            "quote",
            "list",
            "listitem",
            "link",
            "autolink",
            "text",
            "linebreak",
            "tab",
            "code",
            "code-highlight",
            "horizontalrule",
            "image",
        )
    }
}
