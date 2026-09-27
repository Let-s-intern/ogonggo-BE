package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.work24.domain.Work24Item
import com.ogonggo.core.work24.implement.dto.Work24CollectDto
import com.ogonggo.core.work24.implement.dto.Work24ItemAppendDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.LocalDate

/**
 * 고용24 목록을 끝 페이지까지 넘기며 처음 보는 항목을 저장한다.
 *
 * 페이지를 넘기다 다음 중 하나면 멈춘다.
 * - 응답의 전체 건수만큼 받았거나, 전체 건수가 없고 한 페이지를 다 채우지 못했다.
 * - 항목이 없거나 이번 수집에서 이미 받은 항목만 왔다. 페이지 파라미터를 무시하는 API가 같은 페이지를 되풀이하는 경우다.
 * - 고용24 페이지 상한(1,000쪽)에 닿았다.
 *
 * 페이지를 저장할 때마다 반영되므로 중간에 실패해도 그 전까지 받은 항목은 남는다.
 */
@Component
class Work24Collector(
    private val work24Client: Work24Client,
    private val work24ItemAppender: Work24ItemAppender,
    private val objectMapper: ObjectMapper,
) {

    fun isConfigured(target: Work24CollectionTarget): Boolean = work24Client.isConfigured(target.api.service)

    fun collect(target: Work24CollectionTarget, today: LocalDate): Work24CollectDto {
        val baseParameters = target.parameters(today)
        val collectedIds = mutableSetOf<String>()
        var receivedCount = 0
        var pageCount = 0
        var fetchedCount = 0
        var appendedCount = 0

        for (page in 1..MAX_PAGE) {
            val response = work24Client.fetch(
                target.api,
                baseParameters + mapOf(
                    target.paging.pageParameter to page.toString(),
                    target.paging.sizeParameter to PAGE_SIZE.toString(),
                ),
            )
            pageCount++
            val nodes = items(response, target.itemPath)
            receivedCount += nodes.size
            val newItems = nodes.mapNotNull { toAppendDto(target, it) }.filter { collectedIds.add(it.externalId) }
            if (newItems.isEmpty()) {
                break
            }

            fetchedCount += newItems.size
            appendedCount += work24ItemAppender.appendNew(target.api, newItems)

            val total = response.path(target.paging.totalField).asText().toIntOrNull()
            // 식별값이 없어 건너뛴 항목도 받은 것이므로 전체 건수와는 받은 항목 수로 비교한다.
            val finished = if (total != null) receivedCount >= total else nodes.size < PAGE_SIZE
            if (finished) {
                break
            }
        }

        return Work24CollectDto(
            target = target,
            pageCount = pageCount,
            fetchedCount = fetchedCount,
            appendedCount = appendedCount,
        )
    }

    private fun toAppendDto(target: Work24CollectionTarget, item: JsonNode): Work24ItemAppendDto? {
        val payload = objectMapper.writeValueAsString(item)
        val externalId = if (target.idFields.isEmpty()) {
            sha256(payload)
        } else {
            target.idFields.map { item.path(it).asText().trim() }
                .takeIf { values -> values.all { it.isNotEmpty() } }
                ?.joinToString(ID_SEPARATOR)
        }

        if (externalId == null || externalId.length > Work24Item.EXTERNAL_ID_MAX_LENGTH) {
            log.warn("고용24 항목의 식별값을 만들 수 없어 건너뜁니다. target={}, item={}", target, payload.take(MAX_LOGGED_ITEM))
            return null
        }
        return Work24ItemAppendDto(externalId = externalId, payload = payload)
    }

    companion object {
        /** 고용24 목록 API가 허용하는 최대값이다. */
        private const val PAGE_SIZE = 100
        private const val MAX_PAGE = 1000
        private const val ID_SEPARATOR = "-"
        private const val MAX_LOGGED_ITEM = 300
        private val log = LoggerFactory.getLogger(Work24Collector::class.java)

        /**
         * XML을 옮긴 JSON은 항목이 하나면 객체, 여럿이면 배열이다. 항목이 없으면 요소가 없거나 빈 문자열이다.
         */
        internal fun items(response: JsonNode, itemPath: List<String>): List<JsonNode> {
            val node = itemPath.fold(response) { current, name -> current.path(name) }
            return when {
                node.isArray -> node.toList()
                node.isObject -> listOf(node)
                else -> emptyList()
            }
        }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
