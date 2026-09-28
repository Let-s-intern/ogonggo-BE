package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.LocalDateTime

class Work24CollectorTest {

    private val restClientBuilder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val bootcampReader = Mockito.mock(BootcampReader::class.java)

    /** 저장소 대신 등록한 값을 기록한다. */
    private val appendedJobs = mutableListOf<JobAppendDto>()
    private val appendedBootcamps = mutableListOf<BootcampAppendDto>()
    private val jobAppender = Mockito.mock(JobAppender::class.java) { invocation ->
        appendedJobs += invocation.getArgument<JobAppendDto>(0)
        null
    }
    private val bootcampAppender = Mockito.mock(BootcampAppender::class.java) { invocation ->
        appendedBootcamps += invocation.getArgument<BootcampAppendDto>(0)
        null
    }

    @Test
    fun `채용정보는 이미 등록된 원문은 건너뛰고 새 공고만 상세를 불러 게시한다`() {
        // given
        Mockito.`when`(jobReader.existsBySourceUrl("$WORKNET/K1")).thenReturn(true)
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andExpect(queryParam("callTp", "L"))
            .andExpect(queryParam("regDate", "D-3"))
            .andExpect(queryParam("startPage", "1"))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>2</total>
                      <wanted><wantedAuthNo>K1</wantedAuthNo><wantedInfoUrl>$WORKNET/K1</wantedInfoUrl></wanted>
                      <wanted>
                        <wantedAuthNo>K2</wantedAuthNo><company>목록회사</company><title>목록 제목</title>
                        <region>서울 강남구</region><regDt>26-09-25</regDt><closeDt>26-10-31</closeDt>
                        <wantedInfoUrl>$WORKNET/K2</wantedInfoUrl>
                      </wanted>
                    </wantedRoot>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
            .andExpect(queryParam("callTp", "D"))
            .andExpect(queryParam("infoSvc", "VALIDATION"))
            .andExpect(queryParam("wantedAuthNo", "K2"))
            .andRespond(
                xml(
                    """
                    <wantedDtl>
                      <corpInfo><corpNm>오공고</corpNm><indTpCdNm>소프트웨어 개발</indTpCdNm></corpInfo>
                      <wantedInfo>
                        <wantedTitle>백엔드 개발자</wantedTitle><jobsNm>응용 소프트웨어 개발자</jobsNm>
                        <jobCont>API 개발</jobCont><receiptCloseDt>2026-10-31</receiptCloseDt>
                        <collectPsncnt>2</collectPsncnt><salTpNm>연봉 4000만원 이상</salTpNm>
                        <empTpCd>10</empTpCd><enterTpCd>N</enterTpCd><minEdubgIcd>05</minEdubgIcd>
                        <selMthd>서류 → 면접</selMthd><rcptMthd>워크넷 지원</rcptMthd>
                      </wantedInfo>
                      <empchargeInfo><contactTelno>02-000-0000</contactTelno></empchargeInfo>
                    </wantedDtl>
                    """.trimIndent(),
                ),
            )

        // when
        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        // then
        server.verify()
        assertEquals(1, result.appendedCount)
        assertEquals(1, result.skippedCount)
        val job = appendedJobs.single()
        assertEquals("오공고", job.companyName)
        assertEquals("백엔드 개발자", job.title)
        assertEquals("응용 소프트웨어 개발자", job.jobRole)
        assertEquals("서울 강남구", job.region)
        assertEquals(EmploymentType.FULL_TIME, job.employmentType)
        assertEquals(ExperienceType.NEWCOMER, job.experienceType)
        assertEquals(EducationLevel.BACHELOR, job.educationLevel)
        assertEquals(JobRecruitmentType.PERIOD, job.recruitmentType)
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), job.recruitmentStartAt)
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 59, 59), job.recruitmentEndAt)
        assertEquals(2, job.recruitmentHeadcount)
        assertEquals("API 개발", job.responsibilities)
        assertTrue(job.recruitmentNotice!!.contains("문의 전화: 02-000-0000"))
        assertEquals("$WORKNET/K2", job.sourceUrl)
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
    }

    @Test
    fun `훈련과정은 전체 건수만큼 페이지를 넘기며 과정 정보를 불러 개강일까지 모집하는 부트캠프로 게시한다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("pageNum", "1"))
            .andExpect(queryParam("pageSize", "100"))
            .andExpect(queryParam("srchTraStDt", "20260927"))
            .andExpect(queryParam("srchTraEndDt", "20261226"))
            .andRespond(xml(trainingPage(total = 101, ids = (1..100).map { "C$it" })))
        // 앞 페이지의 과정은 이미 등록되어 있다고 둔다.
        (1..100).forEach { Mockito.`when`(bootcampReader.existsBySourceUrl("$HRD/C$it")).thenReturn(true) }
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("pageNum", "2"))
            // 마지막 페이지에 항목이 하나뿐이면 배열이 아니라 객체로 온다.
            .andRespond(xml(trainingPage(total = 101, ids = listOf("C101"))))
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L02.do")))
            .andExpect(queryParam("outType", "2"))
            .andExpect(queryParam("srchTrprId", "C101"))
            .andExpect(queryParam("srchTrprDegr", "1"))
            .andExpect(queryParam("srchTorgId", "ORG"))
            .andRespond(
                xml(
                    """
                    <HRDNet>
                      <inst_base_info>
                        <inoNm>오공고 아카데미</inoNm><trprNm>자바 백엔드 과정</trprNm><trtm>600</trtm>
                        <ncsNm>응용SW엔지니어링</ncsNm><trprChapEmail>edu@ogonggo.test</trprChapEmail>
                        <filePath>/upload/logo/</filePath><pFileName>academy.png</pFileName>
                      </inst_base_info>
                      <inst_detail_info><tgcrGnrlTrneOwepAllt>0</tgcrGnrlTrneOwepAllt></inst_detail_info>
                    </HRDNet>
                    """.trimIndent(),
                ),
            )

        // when
        val result = collector().collect(Work24CollectionTarget.TOMORROW_LEARNING_CARD_COURSES, NOW)

        // then
        server.verify()
        assertEquals(2, result.pageCount)
        assertEquals(100, result.skippedCount)
        val bootcamp = appendedBootcamps.single()
        assertEquals("오공고 아카데미", bootcamp.companyName)
        assertEquals("자바 백엔드 과정", bootcamp.title)
        assertEquals(LocalDate.of(2026, 10, 5), bootcamp.programStartDate)
        assertEquals(BootcampRecruitmentType.PERIOD, bootcamp.recruitmentType)
        assertEquals(NOW, bootcamp.recruitmentStartAt)
        assertEquals(LocalDateTime.of(2026, 10, 5, 23, 59, 59), bootcamp.recruitmentEndAt)
        assertEquals("https://www.work24.go.kr/upload/logo/academy.png", bootcamp.logoUrl)
        assertEquals(bootcamp.logoUrl, bootcamp.representativeImageUrl)
        assertEquals("응용SW엔지니어링 · 총 600시간", bootcamp.shortDescription)
        assertEquals("$HRD/C101", bootcamp.applicationUrl)
        assertEquals("edu@ogonggo.test", bootcamp.managerEmail)
    }

    @Test
    fun `페이지 파라미터를 무시하고 같은 목록을 되풀이하면 멈춘다`() {
        // 전체 건수가 없고 페이지가 가득 차 있어 다음 페이지를 요청하게 된다.
        val samePage = "<wantedRoot>" +
            (1..100).joinToString("") { "<wanted><wantedAuthNo>K$it</wantedAuthNo><wantedInfoUrl>$WORKNET/K$it</wantedInfoUrl></wanted>" } +
            "</wantedRoot>"
        (1..100).forEach { Mockito.`when`(jobReader.existsBySourceUrl("$WORKNET/K$it")).thenReturn(true) }
        repeat(2) {
            server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do"))).andRespond(xml(samePage))
        }

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        server.verify()
        assertEquals(2, result.pageCount)
        assertEquals(100, result.skippedCount)
    }

    @Test
    fun `한 항목이 실패해도 다음 항목을 등록한다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>2</total>
                      <wanted><wantedAuthNo>K1</wantedAuthNo></wanted>
                      <wanted><wantedAuthNo>K2</wantedAuthNo><company>회사</company><title>제목</title><wantedInfoUrl>$WORKNET/K2</wantedInfoUrl></wanted>
                    </wantedRoot>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
            .andRespond(xml("<wantedDtl><wantedInfo><jobCont>업무</jobCont></wantedInfo></wantedDtl>"))

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        // 원문 URL이 없는 첫 항목은 실패로 세고, 두 번째 항목은 목록 값으로 등록한다.
        assertEquals(1, result.failedCount)
        assertEquals(1, result.appendedCount)
        assertEquals(JobRecruitmentType.ALWAYS_OPEN, appendedJobs.single().recruitmentType)
    }

    @Test
    fun `상세 조회가 연달아 실패하면 고용24 장애로 보고 멈춘다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    "<wantedRoot><total>20</total>" +
                        (1..20).joinToString("") { "<wanted><wantedAuthNo>K$it</wantedAuthNo><wantedInfoUrl>$WORKNET/K$it</wantedInfoUrl></wanted>" } +
                        "</wantedRoot>",
                ),
            )
        repeat(10) {
            server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do"))).andRespond(withServerError())
        }

        assertThrows(IllegalStateException::class.java) {
            collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)
        }
        server.verify()
    }

    @Test
    fun `로고가 없는 기관은 대체 이미지를 쓰고 대체 이미지도 없으면 그 과정만 등록하지 않는다`() {
        // given
        repeat(2) {
            server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313L01.do")))
                .andRespond(xml(trainingPage(total = 1, ids = listOf("W1"))))
            server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313D01.do")))
                .andRespond(xml("<HRDNet><inst_base_info><inoNm>기관</inoNm></inst_base_info></HRDNet>"))
        }

        // when
        val withFallback = collector().collect(Work24CollectionTarget.WORK_STUDY_COURSES, NOW)
        val withoutFallback = collector(imageUrl = "").collect(Work24CollectionTarget.WORK_STUDY_COURSES, NOW)

        // then
        assertEquals(1, withFallback.appendedCount)
        assertEquals(IMAGE_URL, appendedBootcamps.single().representativeImageUrl)
        assertNull(appendedBootcamps.single().logoUrl)
        assertEquals(0, withoutFallback.appendedCount)
        assertEquals(1, withoutFallback.noImageCount)
        assertEquals(0, withoutFallback.failedCount)
    }

    @Test
    fun `로고 경로와 파일명을 이어 로고 주소를 만든다`() {
        fun logo(path: String?, fileName: String?): String? {
            val base = listOfNotNull(
                path?.let { "<filePath>$it</filePath>" },
                fileName?.let { "<pFileName>$it</pFileName>" },
            ).joinToString("")
            val detail = Work24XmlConverter.toJson("<HRDNet><inst_base_info>$base</inst_base_info></HRDNet>")
            return Work24BootcampMapper.logoUrl(detail, "https://files.test/")
        }

        assertEquals("https://files.test/upload/a.png", logo("/upload", "a.png"))
        assertEquals("https://files.test/upload/a.png", logo("/upload/a.png", "a.png"))
        assertEquals("https://cdn.test/a.png", logo("https://cdn.test/", "a.png"))
        assertNull(logo("/upload", null))
        assertNull(logo("upload", "a.png"))
    }

    private fun collector(imageUrl: String = IMAGE_URL): Work24Collector {
        val properties = Work24Properties(
            baseUrl = BASE_URL,
            recruitmentAuthKey = "recruitment-key",
            tomorrowLearningCardAuthKey = "training-key",
            workStudyAuthKey = "work-study-key",
            trainingFileBaseUrl = "https://www.work24.go.kr",
            bootcampImageUrl = imageUrl,
        )
        return Work24Collector(
            Work24Client(restClientBuilder.build(), properties, ObjectMapper()),
            properties,
            jobReader,
            jobAppender,
            bootcampReader,
            bootcampAppender,
        )
    }

    private fun trainingPage(total: Int, ids: List<String>): String =
        "<HRDNet><scn_cnt>$total</scn_cnt><srchList>" +
            ids.joinToString("") { id ->
                "<scn_list><trprId>$id</trprId><trprDegr>1</trprDegr><trainstCstId>ORG</trainstCstId>" +
                    "<title>목록 과정명</title><subTitle>목록 기관명</subTitle><titleLink>$HRD/$id</titleLink>" +
                    "<traStartDate>2026-10-05</traStartDate><traEndDate>2027-03-31</traEndDate></scn_list>"
            } +
            "</srchList></HRDNet>"

    private fun xml(body: String) = withSuccess(body, MediaType.APPLICATION_XML)

    private companion object {
        const val BASE_URL = "https://work24.test/cm/openApi/call"
        const val WORKNET = "https://www.work24.go.kr/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo="
        const val HRD = "https://www.work24.go.kr/hr/a/a/3100/selectTracseDetl.do?tracseId="
        const val IMAGE_URL = "https://cdn.ogonggo.test/work24-bootcamp.png"
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 27, 4, 0)
    }
}
