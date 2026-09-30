package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24InstitutionImagesDto
import com.ogonggo.core.storage.s3.S3ImageStorage
import com.ogonggo.core.storage.s3.S3ImageStorageProperties
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 고용24 훈련기관 소개 화면의 로고와 훈련기관 사진 첫 장을 오공고 이미지 저장소(S3)로 옮긴다.
 *
 * Open API는 이미지를 주지 않고 과정·기관정보의 로고 주소(`filePath`)는 이미지로 열리지 않는다.
 * 훈련기관 소개 화면(목록의 `subTitleLink`, `selectTrainInstitution.do`)의 이미지는 로그인 없이 받아지지만
 * 고용24가 파일 내려받기 형식(`application/octet-stream`, 캐시 금지)으로 주고 원본이 수 MB라 화면에 직접 걸지 않는다.
 *
 * - 저장 키는 원본 주소의 해시라 같은 이미지를 다시 옮겨도 같은 자리에 덮어쓴다. 사용자가 올린 이미지(`image_assets`)가 아니라
 *   행을 만들지 않으며, 고아 이미지 정리 대상도 아니다.
 * - 같은 훈련기관의 과정이 여럿이라 기관별 결과를 기억해 두고 다시 받지 않는다. 기억은 애플리케이션이 떠 있는 동안만 유지한다.
 * - 저장소(bucket)가 설정되지 않았으면 고용24를 부르지 않고 null을 돌려준다.
 * - 화면은 명세가 없고 고용24가 바꿀 수 있어, 실패하면 예외를 내지 않고 옮긴 데까지만 돌려준다.
 */
@Component
class Work24InstitutionImageImporter(
    @Qualifier(WORK24_REST_CLIENT)
    private val work24RestClient: RestClient,
    private val s3ImageStorage: S3ImageStorage,
    private val s3ImageStorageProperties: S3ImageStorageProperties,
) {

    private val imported = ConcurrentHashMap<String, Work24InstitutionImagesDto>()

    /** [institutionId]는 목록의 훈련기관 ID(`trainstCstId`), [institutionUrl]은 훈련기관 소개 화면 주소(`subTitleLink`)다. */
    fun import(institutionId: String?, institutionUrl: String?): Work24InstitutionImagesDto? {
        if (institutionId == null || institutionUrl == null || s3ImageStorageProperties.bucket.isBlank()) {
            return null
        }
        imported[institutionId]?.let { return it }

        val pageUri = institutionPageUri(institutionUrl) ?: return null
        val page = attempt("소개 화면", institutionUrl) {
            Jsoup.parse(work24RestClient.get().uri(pageUri).retrieve().body(ByteArray::class.java)?.toString(Charsets.UTF_8).orEmpty())
        } ?: return null

        val logo = page.selectFirst("img[alt=$LOGO_ALT]")?.attr("src")
        val photo = page.selectFirst(".thumbnailIntroList .swiper-slide img")?.attr("src")
        val images = Work24InstitutionImagesDto(
            logoUrl = logo?.let { move(pageUri, it) },
            representativeImageUrl = photo?.let { move(pageUri, it) },
        )
        // 하나도 옮기지 못했으면 기억하지 않고 다음 과정에서 다시 시도한다.
        if (images.logoUrl != null || images.representativeImageUrl != null) {
            imported[institutionId] = images
        }
        return images
    }

    private fun move(pageUri: URI, source: String): String? {
        val imageUri = attempt("이미지 주소", source) { pageUri.resolve(source) }
            ?.takeIf { it.host == HOST && it.path == IMAGE_PATH }
            ?: return null
        return attempt("이미지", imageUri.toString()) {
            val content = work24RestClient.get().uri(imageUri).retrieve().body(ByteArray::class.java)
            val image = content?.takeIf { it.size <= MAX_DOWNLOAD_BYTES }?.let(Work24ImageShrinker::shrink)
            image?.let { s3ImageStorage.put("$KEY_PREFIX${sha256(imageUri.toString())}.${it.extension}", it.content, it.mimeType) }
        }
    }

    private fun <T> attempt(part: String, url: String, block: () -> T): T? =
        try {
            block()
        } catch (exception: Exception) {
            log.warn("고용24 훈련기관 {}을(를) 옮기지 못해 비워 둡니다. url={}", part, url, exception)
            null
        }

    companion object {
        private const val HOST = "www.work24.go.kr"
        private const val INSTITUTION_PAGE_PATH = "/hr/a/a/3200/selectTrainInstitution.do"
        private const val IMAGE_PATH = "/hr/z/z/0000/hrdFileDownLoad.do"
        private const val LOGO_ALT = "훈련기관사진"

        /** 사용자가 올린 이미지와 같은 `images/` 아래에 두어 같은 공개 경로로 읽힌다. */
        private const val KEY_PREFIX = "images/work24/"
        private const val MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024
        private val log = LoggerFactory.getLogger(Work24InstitutionImageImporter::class.java)

        private fun institutionPageUri(url: String): URI? =
            runCatching { URI.create(url) }.getOrNull()
                ?.takeIf { it.scheme == "https" && it.host == HOST && it.path == INSTITUTION_PAGE_PATH }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(40)
    }
}
