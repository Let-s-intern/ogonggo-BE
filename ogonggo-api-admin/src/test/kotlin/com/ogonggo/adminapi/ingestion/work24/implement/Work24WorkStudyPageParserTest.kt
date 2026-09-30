package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24WorkStudyPageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Work24WorkStudyPageParserTest {

    @Test
    fun `일학습병행 상세 화면에서 학습기업과 훈련목적과 현장 교육훈련 편성을 읽고 뜻 없는 값은 비운다`() {
        assertEquals(
            Work24WorkStudyPageDto(
                company = "(주)화인기업",
                purpose = "선박도장에 필요한 능력을 함양한다.",
                mainContent = null,
                requirement = null,
                ojtHours = "12개월 / 360일 / 총600시간",
                offJtHours = "12개월 / 360일 / 총200시간",
                ojtSubjects = listOf(
                    Work24WorkStudyPageDto.Subject("선박도장실무 1", "선박도장 터치업 도장", "필수", "80 시간"),
                    Work24WorkStudyPageDto.Subject("SQL활용", "SQL활용", "선택", "40 시간"),
                ),
            ),
            Work24WorkStudyPageParser.parse(WORK24_WORK_STUDY_PAGE_HTML),
        )
    }

    @Test
    fun `화면 구조가 달라 값을 못 찾으면 모두 비운다`() {
        assertEquals(Work24WorkStudyPageDto(), Work24WorkStudyPageParser.parse("<html><body><p>점검 중입니다.</p></body></html>"))
    }
}

/**
 * 고용24 일학습병행 과정 상세 화면에서 읽는 부분만 남긴 HTML이다.
 * 실제 화면처럼 주요 훈련 내용·훈련 대상 요건은 안내 문구뿐이고, 사업장 외 교육훈련은 훈련시간만 있다.
 */
internal val WORK24_WORK_STUDY_PAGE_HTML = """
    <html><body>
    <p class="corp_info"><strong class="mr08">(주)화인기업</strong></p>
    <div id="section_ojt">
      <table><caption> 훈련정보 </caption><tbody>
        <tr><th>관할지부지사</th><td>경남지사</td><th>관할고용센터</th><td>통영고용센터</td></tr>
        <tr><th>
            훈련시간
        </th><td colspan='3'>
            12개월 /
            360일 /
            총600시간
        </td></tr>
      </tbody></table>
      <table><caption> 훈련내용 </caption><tbody>
        <tr><th>훈련목적</th><td>선박도장에 필요한 능력을 함양한다.</td></tr>
        <tr><th>주요 훈련 내용</th><td>PDMS 시스템 개발보고서 참고</td></tr>
        <tr><th>훈련 대상 요건</th><td>PDMS 시스템 개발보고서 참고</td></tr>
      </tbody></table>
      <table id="schlsbjTable"><caption>훈련 편성 정보</caption>
        <thead><tr><th>교과목</th><th>능력단위명</th><th>NCS코드</th><th>필수여부</th><th>훈련시간</th></tr></thead>
        <tbody>
          <tr><td>선박도장실무 1 </td><td>선박도장 터치업 도장 </td><td>1508020313_23v4</td><td>필수</td><td>80 시간</td></tr>
          <tr><td>SQL활용</td><td>SQL활용</td><td>-</td><td>선택</td><td>40 시간</td></tr>
        </tbody>
      </table>
    </div>
    <div id="section_offjt">
      <table><caption> 훈련정보 </caption><tbody>
        <tr><th>훈련시간</th><td colspan='3'>12개월 / 360일 / 총200시간</td></tr>
      </tbody></table>
      <table><caption> 훈련내용 </caption><tbody><tr><th>훈련목적</th><td>-</td></tr></tbody></table>
      <table><caption>훈련 편성 정보</caption>
        <tbody><tr><td colspan="5">자료가 없습니다. 다른 검색조건을 선택해주세요</td></tr></tbody>
      </table>
    </div>
    </body></html>
""".trimIndent()
