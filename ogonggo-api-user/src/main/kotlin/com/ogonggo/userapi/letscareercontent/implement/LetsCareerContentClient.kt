package com.ogonggo.userapi.letscareercontent.implement

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentSource
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncDto
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

@Component
class LetsCareerContentClient(
    private val letsCareerRestClient: RestClient,
    private val properties: LetsCareerProperties,
) {

    /**
     * 렛츠커리어가 지금 노출 중인 콘텐츠(프로그램·무료 자료집·블로그) 전체를 가져온다.
     * 실패하면 [org.springframework.web.client.RestClientException]을 그대로 던진다. 받은 목록으로 사본을 덮어쓰므로
     * 실패를 빈 목록으로 바꾸면 사본이 모두 지워진다.
     *
     * 오공고가 모르는 종류나 식별자·제목·경로가 없는 항목은 뺀다.
     */
    fun readCatalog(): List<LetsCareerContentSyncDto> {
        val response = letsCareerRestClient.get()
            .uri(CATALOG_PATH)
            .header(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
            .retrieve()
            .body(object : ParameterizedTypeReference<CatalogApiResponse>() {})

        val items = response?.data?.contentList.orEmpty()
        val contents = items.mapNotNull(CatalogContentResponse::toDtoOrNull)
        if (contents.size < items.size) {
            log.warn("렛츠커리어 콘텐츠 중 읽지 못한 항목을 뺐습니다. received={}, accepted={}", items.size, contents.size)
        }
        return contents
    }

    companion object {
        private const val CATALOG_PATH = "/api/v1/internal/catalog"
        private const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        private val log = LoggerFactory.getLogger(LetsCareerContentClient::class.java)
    }
}

internal data class CatalogApiResponse(
    val data: CatalogResponse?,
)

internal data class CatalogResponse(
    val contentList: List<CatalogContentResponse>?,
)

/** 렛츠커리어 `CatalogContentVo`의 모양이다. */
internal data class CatalogContentResponse(
    val contentType: String?,
    val programType: String?,
    val category: String?,
    val contentId: Long?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val path: String?,
    val labels: List<String>?,
    val beginning: LocalDateTime?,
    val deadline: LocalDateTime?,
) {
    fun toDtoOrNull(): LetsCareerContentSyncDto? {
        val kind = kindOf(contentType, programType) ?: return null
        if (contentId == null || contentId <= 0 || title.isNullOrBlank() || path.isNullOrBlank() || !path.startsWith("/")) {
            return null
        }
        return LetsCareerContentSyncDto(
            kind = kind,
            externalId = contentId,
            source = LetsCareerContentSource(
                category = category?.takeIf(String::isNotBlank),
                title = title,
                description = description?.takeIf(String::isNotBlank),
                thumbnailUrl = thumbnail?.takeIf(String::isNotBlank),
                path = path,
                labels = labels.orEmpty(),
                recruitmentStartAt = beginning,
                recruitmentEndAt = deadline,
            ),
        )
    }

    private fun kindOf(contentType: String?, programType: String?): LetsCareerContentKind? = when (contentType) {
        "MATERIAL" -> LetsCareerContentKind.MATERIAL
        "BLOG" -> LetsCareerContentKind.BLOG
        "PROGRAM" -> when (programType) {
            "CHALLENGE" -> LetsCareerContentKind.CHALLENGE
            "LIVE" -> LetsCareerContentKind.LIVE
            "VOD" -> LetsCareerContentKind.VOD
            "GUIDEBOOK" -> LetsCareerContentKind.GUIDEBOOK
            "REPORT" -> LetsCareerContentKind.REPORT
            else -> null
        }
        else -> null
    }
}
