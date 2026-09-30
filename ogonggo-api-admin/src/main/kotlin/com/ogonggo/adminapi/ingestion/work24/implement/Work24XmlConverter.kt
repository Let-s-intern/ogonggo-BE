package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 고용24의 XML 응답을 JSON 트리로 옮긴다.
 *
 * jackson-dataformat-xml을 쓰지 않는 이유는 클래스패스에 올라가는 순간 Spring MVC가
 * XML 메시지 컨버터를 JSON보다 앞에 등록해, `Accept`에 XML이 있는 요청에 우리 API가 XML로 응답하기 때문이다.
 *
 * 변환 규칙은 다음과 같다.
 * - 최상위 요소 이름은 버리고 그 내용만 남긴다. 고용24는 API마다 최상위 이름이 달라 쓸모가 없다.
 * - 자식 요소가 없는 요소는 문자열 값이 된다. 빈 요소는 빈 문자열이다.
 * - 같은 이름의 자식 요소가 여러 번 나오면 배열이 된다. 한 번만 나오면 배열이 아니라 객체 하나다.
 * - 속성은 쓰지 않는다. 고용24 응답에는 속성이 없다.
 */
internal object Work24XmlConverter {

    private val nodeFactory = JsonNodeFactory.instance

    fun toJson(xml: String): JsonNode = convert(parse(InputSource(StringReader(xml))).documentElement)

    /** 외부에서 받은 XML은 모두 이 설정으로 읽는다. 시간표 엑셀 안의 XML도 같다. */
    fun parse(source: InputSource): Document = newDocumentBuilderFactory().newDocumentBuilder().parse(source)

    private fun convert(element: Element): JsonNode {
        val children = childElements(element)
        if (children.isEmpty()) {
            return nodeFactory.textNode(element.textContent.trim())
        }

        val result: ObjectNode = nodeFactory.objectNode()
        children.forEach { child ->
            val name = child.tagName
            val value = convert(child)
            when (val existing = result.get(name)) {
                null -> result.set<JsonNode>(name, value)
                is ArrayNode -> existing.add(value)
                else -> result.set<JsonNode>(name, nodeFactory.arrayNode().add(existing).add(value))
            }
        }
        return result
    }

    private fun childElements(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length)
            .map(nodes::item)
            .filter { it.nodeType == Node.ELEMENT_NODE }
            .map { it as Element }
    }

    /** 외부 응답을 파싱하므로 DTD와 외부 엔티티를 막아 XXE를 차단한다. */
    private fun newDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
}
