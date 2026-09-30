package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24WorkStudyPageDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.net.URI

/**
 * 고용24 훈련과정 상세 화면에서 Open API에 없는 값을 읽어 온다.
 *
 * 부트캠프로 넣는 국민내일배움카드 훈련과정은 [read]로, 채용공고로 넣는 일학습병행 훈련과정은 [readWorkStudy]로 읽는다.
 * 일학습병행은 상세 화면 한 번만 부르고, 국민내일배움카드는 과정 하나에 세 번 호출한다.
 * 1. 상세 화면(목록의 `titleLink`): 훈련과정 개요, 훈련강사, 훈련교재
 * 2. 교과편성 화면: 교과목별 세부내용과 시간
 * 3. 시간표 엑셀: 날짜별 교과목. 화면은 날짜마다 따로 불러야 하지만 엑셀은 전체가 한 번에 온다.
 *
 * 화면은 Open API와 달리 명세와 인증키가 없고 고용24가 예고 없이 바꿀 수 있다.
 * 그래서 어느 호출이 실패하든 예외를 내지 않고 읽은 데까지만 돌려준다. 상세 화면을 못 읽으면 null이다.
 * 목록이 준 링크가 각 화면의 주소가 아니면 부르지 않고 null이다.
 */
@Component
class Work24CoursePageReader(
    @Qualifier(WORK24_REST_CLIENT)
    private val work24RestClient: RestClient,
) {

    fun read(sourceUrl: String): Work24CoursePageDto? {
        val pageUri = pageUri(sourceUrl, COURSE_PAGE_PATH) ?: return null
        val html = attempt("상세 화면", sourceUrl) { text(work24RestClient.get().uri(pageUri).retrieve()) } ?: return null
        val page = attempt("상세 화면", sourceUrl) { Work24CoursePageParser.parse(html) } ?: return null
        val parameters = Work24CoursePageParser.curriculumParameters(html)

        return Work24CoursePageDto(
            overview = attempt("훈련과정 개요", sourceUrl) { Work24CoursePageParser.overview(page) }.orEmpty(),
            instructors = attempt("훈련강사", sourceUrl) { Work24CoursePageParser.instructors(page) }.orEmpty(),
            textbooks = attempt("훈련교재", sourceUrl) { Work24CoursePageParser.textbooks(page) }.orEmpty(),
            subjects = attempt("교과편성", sourceUrl) { subjects(pageUri, parameters) }.orEmpty(),
            lessons = attempt("시간표", sourceUrl) { lessons(pageUri, parameters) }.orEmpty(),
        )
    }

    /** 일학습병행 상세 화면에서 학습기업 이름, 훈련목적, 현장 교육훈련 편성을 읽는다. */
    fun readWorkStudy(sourceUrl: String): Work24WorkStudyPageDto? {
        val pageUri = pageUri(sourceUrl, WORK_STUDY_PAGE_PATH) ?: return null
        return attempt("일학습병행 상세 화면", sourceUrl) {
            Work24WorkStudyPageParser.parse(text(work24RestClient.get().uri(pageUri).retrieve()))
        }
    }

    private fun subjects(pageUri: URI, parameters: Map<String, String>): List<Work24CoursePageDto.Subject> {
        if (parameters.isEmpty()) {
            return emptyList()
        }
        val html = text(post(pageUri.resolve(CURRICULUM_PATH), parameters))
        return Work24CoursePageParser.subjects(html)
    }

    private fun lessons(pageUri: URI, parameters: Map<String, String>): List<Work24CoursePageDto.Lesson> {
        val courseId = parameters[COURSE_ID] ?: return emptyList()
        val round = parameters[COURSE_ROUND] ?: return emptyList()
        val xlsx = post(pageUri.resolve(TIMETABLE_PATH), mapOf(COURSE_ID to courseId, COURSE_ROUND to round))
            .body(ByteArray::class.java)
            ?: return emptyList()
        return Work24TimetableParser.lessons(xlsx)
    }

    private fun post(uri: URI, form: Map<String, String>): RestClient.ResponseSpec =
        work24RestClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(LinkedMultiValueMap<String, String>().apply { form.forEach { (name, value) -> add(name, value) } })
            .retrieve()

    /** 화면은 UTF-8이다. 응답 헤더에 charset이 빠져도 한글이 깨지지 않도록 직접 디코딩한다. */
    private fun text(response: RestClient.ResponseSpec): String =
        response.body(ByteArray::class.java)?.toString(Charsets.UTF_8).orEmpty()

    private fun <T> attempt(part: String, sourceUrl: String, block: () -> T): T? =
        try {
            block()
        } catch (exception: Exception) {
            log.warn("고용24 과정 {}을(를) 읽지 못해 비워 둡니다. url={}", part, sourceUrl, exception)
            null
        }

    companion object {
        private const val HOST = "www.work24.go.kr"
        private const val COURSE_PAGE_PATH = "/hr/a/a/3100/selectTracseDetl.do"
        private const val WORK_STUDY_PAGE_PATH = "/hr/a/a/3100/selectJobAndStudyTracseDetail.do"
        private const val CURRICULUM_PATH = "/hr/a/a/3100/selectCurriculum.do"
        private const val TIMETABLE_PATH = "/hr/a/a/3100/selectTimeTableExcelDownload.do"
        private const val COURSE_ID = "tracseId"
        private const val COURSE_ROUND = "tracseTme"
        private val log = LoggerFactory.getLogger(Work24CoursePageReader::class.java)

        /** 목록이 준 링크가 고용24의 해당 상세 화면일 때만 부른다. */
        private fun pageUri(sourceUrl: String, path: String): URI? =
            runCatching { URI.create(sourceUrl) }.getOrNull()
                ?.takeIf { it.scheme == "https" && it.host == HOST && it.path == path }
    }
}
