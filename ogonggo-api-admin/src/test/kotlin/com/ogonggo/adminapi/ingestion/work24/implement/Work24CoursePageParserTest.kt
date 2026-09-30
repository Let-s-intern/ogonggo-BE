package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Work24CoursePageParserTest {

    @Test
    fun `상세 화면에서 개요와 강사와 교재를 줄바꿈을 살려 읽는다`() {
        val page = Work24CoursePageParser.parse(WORK24_COURSE_PAGE_HTML)

        assertEquals(
            mapOf(
                "훈련목표" to "- 웹 서비스를 구현할 수 있다.\n- 배포할 수 있다.",
                "훈련대상 요건 선수학습" to "특별한 선수학습 없음",
                "훈련대상 요건 훈련과정의 장점" to "■ 프로젝트 기반 학습",
            ),
            Work24CoursePageParser.overview(page),
        )
        assertEquals(
            listOf(Work24CoursePageDto.Instructor("민*식", "전기정보공", "정보처리기사")),
            Work24CoursePageParser.instructors(page),
        )
        assertEquals(listOf("쉽게 시작하는 쿠버네티스"), Work24CoursePageParser.textbooks(page))
    }

    @Test
    fun `교과편성 화면을 여는 값은 상세 화면 스크립트에서 읽는다`() {
        assertEquals(
            mapOf("hpgYn" to "Y", "tracseId" to "C101", "tracseTme" to "1", "ncsYn" to "N"),
            Work24CoursePageParser.curriculumParameters(WORK24_COURSE_PAGE_HTML),
        )
    }

    @Test
    fun `교과편성은 교과목명과 세부내용과 시간을 읽는다`() {
        assertEquals(
            listOf(
                Work24CoursePageDto.Subject("프로젝트", "① 주제1\n- 맛집 리뷰 서비스", "200 시간"),
                Work24CoursePageDto.Subject("자바", "- 기초 문법", "139 시간"),
            ),
            Work24CoursePageParser.subjects(WORK24_CURRICULUM_HTML),
        )
    }

    @Test
    fun `NCS를 적용한 과정의 교과편성은 세부내용이 없어 읽지 않는다`() {
        val html = """
            <table>
              <thead><tr><th>교과목</th><th>NCS 능력단위(요소)</th><th>수준</th><th>훈련강사</th></tr></thead>
              <tbody><tr><td>UI구현</td><td>UI 구현</td><td>3</td><td>홍길동[200102]</td></tr></tbody>
            </table>
        """.trimIndent()

        assertTrue(Work24CoursePageParser.subjects(html).isEmpty())
    }
}

/** 고용24 과정 상세 화면에서 읽는 부분만 남긴 HTML이다. 숨은 입력값의 담당자 연락처는 읽지 않아야 한다. */
internal val WORK24_COURSE_PAGE_HTML = """
    <html><body>
    <form id="searchForm1"><input type="hidden" name="cherCryalTelno" value="010-0000-0000"/></form>
    <script>
      function fn_PopOpen(){
        var paramData = {
          "hpgYn": "Y",
          "tracseId": 'C101',
          "tracseTme": '1',
          "ncsYn": 'N',
        }
        ComFnLib.fn_openLayerPopup("hphraa3100p01", '/hr/a/a/3100/selectCurriculum.do' , paramData, null);
      }
    </script>
    <div id="traCrseinfo"><table>
      <caption>훈련과정 개요 상세 표</caption>
      <tbody>
        <tr><th>훈련목표</th><td>
            - 웹 서비스를 구현할 수 있다.
<br>- 배포할 수 있다.
        </td></tr>
        <tr><th>훈련대상 요건 선수학습</th><td>특별한 선수학습 없음</td></tr>
        <tr><th>훈련대상 요건<br>훈련과정의 장점</th><td>■ 프로젝트 기반 학습</td></tr>
      </tbody>
    </table></div>
    <table>
      <caption>훈련교재 표이며 교재명, 저자, 발행년도가 포함되있습니다.</caption>
      <thead><tr><th>교재명</th><th>저자</th><th>발행년도</th></tr></thead>
      <tbody><tr><td>쉽게 시작하는 쿠버네티스</td><td>서지영</td><td>2023</td></tr></tbody>
    </table>
    <table>
      <caption>훈련강사 표로 성명, 전공, 자격 정보를 나타냄.</caption>
      <thead><tr><th>성명</th><th>전공</th><th>자격</th></tr></thead>
      <tbody><tr><td>
          민*식
      </td><td>전기정보공</td><td>정보처리기사</td></tr></tbody>
    </table>
    </body></html>
""".trimIndent()

/** 교과편성 화면은 수업 순서와 상관없이 교과목을 늘어놓고, 세부내용의 줄을 원문 줄바꿈으로 나눈다. */
internal val WORK24_CURRICULUM_HTML = """
    <table>
      <thead><tr><th>교과목명</th><th>세부내용</th><th>훈련시간</th></tr></thead>
      <tbody>
        <tr><td>프로젝트</td><td>① 주제1
- 맛집 리뷰 서비스</td><td>200 시간</td></tr>
        <tr><td>자바</td><td>- 기초 문법</td><td>139 시간</td></tr>
      </tbody>
      <tfoot><tr><th colspan="2">총 훈련시간</th><td>339 시간</td></tr></tfoot>
    </table>
""".trimIndent()
