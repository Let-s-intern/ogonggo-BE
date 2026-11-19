package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.error.InternalServerException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Authenticated HTTP boundary for the shared PostHog endpoint used by both API applications. */
class PosthogEndpointsClient(
    private val restClient: RestClient,
    private val properties: PosthogEndpointsProperties,
    private val clock: Clock,
) {
    private val cache = ConcurrentHashMap<String, CachedEndpointResponse>()

    fun run(request: PosthogEndpointsRequest): PosthogEndpointsResponse {
        if (!properties.isConfigured()) sourceUnavailable()
        val baseUri = UriComponentsBuilder.fromUriString(properties.apiHost.trimEnd('/'))
            .path("/api/environments/{projectId}/endpoints/{endpointName}/run")
            .queryParam("version", properties.reportVersion)
            .buildAndExpand(properties.projectId, properties.endpointName)
            .encode()
            .toUriString()
        val cacheKey = cacheKey(baseUri, request)
        cached(cacheKey)?.let { return it }

        val response = fetchAllPages(baseUri, request)
        cache(cacheKey, response)
        return response
    }

    private fun fetchAllPages(baseUri: String, request: PosthogEndpointsRequest): PosthogEndpointsResponse {
        val allRows = mutableListOf<List<com.fasterxml.jackson.databind.JsonNode>>()
        var columns: List<String>? = null
        var offset = 0
        while (true) {
            val page = try {
                restClient.post()
                    .uri(baseUri)
                    .headers { headers ->
                        headers.setBearerAuth(properties.apiKey)
                        headers.contentType = MediaType.APPLICATION_JSON
                        headers.accept = listOf(MediaType.APPLICATION_JSON)
                    }
                    .body(request.copy(limit = properties.pageSize, offset = offset))
                    .retrieve()
                    .body(PosthogEndpointsResponse::class.java)
            } catch (exception: RestClientResponseException) {
                log.warn("PostHog 광고 리포트 endpoint 응답 실패. status={}", exception.statusCode.value())
                sourceUnavailable()
            } catch (_: RestClientException) {
                log.warn("PostHog 광고 리포트 endpoint 연결 또는 응답 변환에 실패했습니다.")
                sourceUnavailable()
            }
            if (page == null || page.columns.isEmpty() || page.columns.distinct().size != page.columns.size) sourceUnavailable()
            if (columns == null) columns = page.columns else if (columns != page.columns) sourceUnavailable()
            if (page.results.size > properties.pageSize) sourceUnavailable()
            if (page.results.isEmpty()) break
            allRows += page.results
            if (allRows.size > properties.maxRows) {
                log.warn("PostHog 광고 리포트 endpoint 결과 행 상한 초과. maxRows={}", properties.maxRows)
                sourceUnavailable()
            }
            if (page.results.size < properties.pageSize) break
            offset += page.results.size
        }
        return PosthogEndpointsResponse(results = allRows, columns = columns ?: sourceUnavailable())
    }

    private fun cached(key: String): PosthogEndpointsResponse? {
        val entry = cache[key] ?: return null
        if (entry.expiresAt <= clock.instant()) {
            cache.remove(key, entry)
            return null
        }
        return entry.response.deepCopy()
    }

    private fun cache(key: String, response: PosthogEndpointsResponse) {
        val now = clock.instant()
        cache.entries.removeIf { it.value.expiresAt <= now }
        if (cache.size >= MAX_CACHE_ENTRIES) cache.keys.firstOrNull()?.let(cache::remove)
        cache[key] = CachedEndpointResponse(response.deepCopy(), now.plus(properties.cacheTtl))
    }

    private fun cacheKey(uri: String, request: PosthogEndpointsRequest): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$uri\n${request.variables}\n${request.variables.exposureIds}".toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun sourceUnavailable(): Nothing {
        // Do not log request/response bodies, bearer credentials, query variables, or exception messages.
        log.warn("광고 성과 리포트 원천을 사용할 수 없습니다.")
        throw InternalServerException(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE)
    }

    private fun PosthogEndpointsResponse.deepCopy() = copy(results = results.map { row -> row.map { it.deepCopy() } })

    private data class CachedEndpointResponse(val response: PosthogEndpointsResponse, val expiresAt: Instant)

    private companion object {
        const val MAX_CACHE_ENTRIES = 512
        val log = LoggerFactory.getLogger(PosthogEndpointsClient::class.java)
    }
}
