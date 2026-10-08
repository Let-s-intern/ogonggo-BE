package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.util.LinkedMultiValueMap
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
    fun `채용정보는 구인인증번호로 이미 등록한 공고는 건너뛰고 새 공고만 상세를 불러 고용24 경로로 게시한다`() {
        // given
        Mockito.`when`(jobReader.existsByExternalId(ContentSource.WORK24, "K1")).thenReturn(true)
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andExpect(queryParam("callTp", "L"))
            .andExpect(queryParam("regDate", "D-3"))
            .andExpect(queryParam("empTp", "10%7C20"))
            .andExpect(queryParam("startPage", "1"))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>2</total>
                      <wanted><wantedAuthNo>K1</wantedAuthNo><jobsCd>133201</jobsCd><wantedInfoUrl>$WORKNET/K1</wantedInfoUrl></wanted>
                      <wanted>
                        <wantedAuthNo>K2</wantedAuthNo><company>목록회사</company><title>목록 제목</title>
                        <region>서울 강남구</region><strtnmCd>116804166040</strtnmCd><jobsCd>133201</jobsCd><regDt>26-09-25</regDt><closeDt>26-10-31</closeDt>
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
        assertEquals(JobField.IT_DEVELOPMENT, job.jobField)
        assertEquals(JobRole.IT_BACKEND, job.jobRole)
        assertEquals(Region.SEOUL, job.region)
        assertEquals(SubRegion.SEOUL_GANGNAM_GU, job.subRegion)
        assertEquals(JobEmploymentType.FULL_TIME, job.employmentType)
        assertEquals(JobExperienceType.NEWCOMER, job.experienceType)
        assertEquals(JobEducationLevel.BACHELOR, job.educationLevel)
        assertEquals(JobRecruitmentType.PERIOD, job.recruitmentType)
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), job.recruitmentStartAt)
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 59, 59), job.recruitmentEndAt)
        assertEquals(2, job.recruitmentHeadcount)
        assertEquals("API 개발", job.responsibilities)
        assertTrue(job.recruitmentNotice!!.contains("문의 전화: 02-000-0000"))
        assertEquals("$WORKNET/K2", job.sourceUrl)
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
        assertEquals(ContentSource.WORK24, job.source)
        assertEquals("K2", job.externalId)
        assertNull(job.ownerUserId)
    }

    @Test
    fun `마감일은 날짜가 있는 쪽을 쓰고 급여는 목록 값을 쓰며 줄바꿈을 맞춘다`() {
        // given: 실제 고용24 응답처럼 상세 마감일에는 날짜가 없고 목록 마감일에만 날짜가 있다.
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>1</total><wanted>
                      <wantedAuthNo>K1</wantedAuthNo><jobsCd>133201</jobsCd><company>회사</company><title>제목</title>
                      <salTpNm>연봉</salTpNm><sal>3000만원 ~ 3500만원</sal>
                      <regDt>26-09-28</regDt><closeDt>채용시까지  26-10-30</closeDt>
                      <wantedInfoUrl>$WORKNET/K1</wantedInfoUrl>
                    </wanted></wantedRoot>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
            .andRespond(
                xml(
                    "<wantedDtl><wantedInfo><receiptCloseDt>채용시까지</receiptCloseDt>" +
                        "<salTpNm>연봉30,000,000원 이상 ~ 35,000,000원 이하,</salTpNm>" +
                        "<jobCont>[주요업무]&#xd;\n- 설치</jobCont></wantedInfo></wantedDtl>",
                ),
            )

        // when
        collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        // then
        val job = appendedJobs.single()
        assertEquals(JobRecruitmentType.PERIOD, job.recruitmentType)
        assertEquals(LocalDateTime.of(2026, 10, 30, 23, 59, 59), job.recruitmentEndAt)
        assertEquals(true, job.closesWhenFilled)
        assertEquals("연봉 3000만원 ~ 3500만원", job.compensation)
        assertEquals("[주요업무]\n- 설치", job.responsibilities)
    }

    @Test
    fun `K-디지털 트레이닝 조건의 과정을 전체 건수만큼 페이지를 넘기며 받아 개강일까지 모집하는 부트캠프로 게시한다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("crseTracseSe", "C0104"))
            .andExpect(queryParam("pageNum", "1"))
            .andExpect(queryParam("pageSize", "100"))
            .andExpect(queryParam("srchTraStDt", "20260927"))
            .andExpect(queryParam("srchTraEndDt", "20261226"))
            .andRespond(xml(trainingPage(total = 101, ids = (1..100).map { "C$it" })))
        // 앞 페이지의 과정은 이미 등록되어 있다고 둔다.
        (1..100).forEach { Mockito.`when`(bootcampReader.existsByExternalId(ContentSource.WORK24, "C$it-1")).thenReturn(true) }
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("pageNum", "2"))
            // 마지막 페이지에 항목이 하나뿐이면 배열이 아니라 객체로 온다.
            .andRespond(xml(trainingPage(total = 101, ids = listOf("C101"), institutionLink = true)))
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
                        <inoNm>오공고 아카데미</inoNm><trprNm>[LG전자] 자바 백엔드 과정</trprNm><trtm>600</trtm>
                        <ncsNm>응용SW엔지니어링</ncsNm><trprChapEmail>edu@ogonggo.test</trprChapEmail>
                        <traingMthCd>M1005</traingMthCd><instPerTrco>22687500</instPerTrco>
                        <filePath>http://hrd.work24.go.kr/comm/com/fileDownload.do?athfilId=1&amp;athfilSeqNo=1</filePath>
                        <pFileName>캡처.JPG</pFileName>
                      </inst_base_info>
                      <inst_detail_info><tgcrGnrlTrneOwepAllt>600000</tgcrGnrlTrneOwepAllt></inst_detail_info>
                    </HRDNet>
                    """.trimIndent(),
                ),
            )
        // Open API에 없는 값은 과정 상세 화면, 교과편성 화면, 시간표 엑셀에서 읽는다.
        server.expect(requestTo("$HRD/C101")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(WORK24_COURSE_PAGE_HTML, MediaType.TEXT_HTML))
        server.expect(requestTo("$WORK24_SITE/hr/a/a/3100/selectCurriculum.do")).andExpect(method(HttpMethod.POST))
            .andExpect(content().formData(form("hpgYn" to "Y", "tracseId" to "C101", "tracseTme" to "1", "ncsYn" to "N")))
            .andRespond(withSuccess(WORK24_CURRICULUM_HTML, MediaType.TEXT_HTML))
        server.expect(requestTo("$WORK24_SITE/hr/a/a/3100/selectTimeTableExcelDownload.do")).andExpect(method(HttpMethod.POST))
            .andExpect(content().formData(form("tracseId" to "C101", "tracseTme" to "1")))
            .andRespond(
                withSuccess(
                    work24TimetableXlsx(
                        // 2026-10-05는 월요일이다. 자바는 1~2주차, 프로젝트는 2주차와 4주차에 수업이 있다.
                        listOf("2026-10-05", "훈련", "09:00", "10:00", "60.0", "민*식", "501강의실", "(비NCS)자바", ""),
                        listOf("2026-10-05", "점심", "13:00", "14:00", "60.0", "", "-", "", ""),
                        listOf("2026-10-13", "훈련", "09:00", "10:00", "60.0", "민*식", "501강의실", "(비NCS)자바", ""),
                        listOf("2026-10-16", "훈련", "09:00", "10:00", "60.0", "민*식", "501강의실", "(비NCS)프로젝트", ""),
                        listOf("2026-10-26", "훈련", "09:00", "10:00", "60.0", "민*식", "501강의실", "(비NCS)프로젝트", ""),
                    ),
                    MediaType.APPLICATION_OCTET_STREAM,
                ),
            )
        // 훈련기관 소개 화면의 로고와 사진을 오공고 저장소로 옮긴다.
        server.expect(requestTo(INSTITUTION)).andRespond(withSuccess(WORK24_INSTITUTION_PAGE_HTML, MediaType.TEXT_HTML))
        server.expect(requestTo("$WORK24_SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=LOGO&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(152, 90, "png"), MediaType.APPLICATION_OCTET_STREAM))
        server.expect(requestTo("$WORK24_SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=DESK&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(800, 600, "jpg"), MediaType.APPLICATION_OCTET_STREAM))
        server.expect(requestTo("$WORK24_SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=ROOM&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(640, 480, "jpg"), MediaType.APPLICATION_OCTET_STREAM))

        // when
        val result = collector().collect(Work24CollectionTarget.K_DIGITAL_TRAINING_COURSES, NOW)

        // then
        server.verify()
        assertEquals(2, result.pageCount)
        assertEquals(100, result.skippedCount)
        val bootcamp = appendedBootcamps.single()
        assertEquals("오공고 아카데미", bootcamp.companyName)
        assertEquals("[LG전자] 자바 백엔드 과정", bootcamp.title)
        // 과정명 앞 괄호의 기업이 파트너사로 들어가고 기업 연계 과정으로 표시된다.
        assertEquals(listOf("LG전자"), bootcamp.partners.map { it.partnerName })
        assertTrue(bootcamp.enterpriseLinked)
        assertEquals(LocalDate.of(2026, 10, 5), bootcamp.programStartDate)
        assertEquals(BootcampRecruitmentType.PERIOD, bootcamp.recruitmentType)
        assertEquals(NOW, bootcamp.recruitmentStartAt)
        assertEquals(LocalDateTime.of(2026, 10, 5, 23, 59, 59), bootcamp.recruitmentEndAt)
        // 고용24는 과정 이미지를 주지 않고 훈련기관 로고는 이미지로 열리지 않는 다운로드 주소라 비운다.
        assertTrue(requireNotNull(bootcamp.logoUrl).matches(Regex("""$CDN/images/work24/[0-9a-f]{40}\.png""")))
        // 훈련기관 사진은 목록의 대표 이미지로 쓰지 않고 상세에서만 보여 주는 사진으로 넣는다.
        assertNull(bootcamp.representativeImageUrl)
        assertEquals(listOf("안내데스크" to 0, "강의실" to 1), bootcamp.images.map { it.caption to it.displayOrder })
        assertTrue(bootcamp.images.all { it.url.matches(Regex("""$CDN/images/work24/[0-9a-f]{40}\.jpg""")) })
        // 고용24는 K-디지털 트레이닝 조건에 다른 훈련유형도 함께 주므로 목록의 훈련유형 이름을 쓴다.
        assertEquals("국가기간전략산업직종", bootcamp.programType)
        assertEquals(BootcampOperationType.ONLINE, bootcamp.operationType)
        // 총 훈련비가 아니라 교육생이 내는 본인부담액이다.
        assertEquals(600_000L, bootcamp.tuitionAmount)
        assertEquals(ContentSource.WORK24, bootcamp.source)
        assertEquals("C101-1", bootcamp.externalId)
        assertEquals("응용SW엔지니어링 · 총 600시간", bootcamp.shortDescription)
        assertEquals("$HRD/C101", bootcamp.applicationUrl)
        assertEquals("edu@ogonggo.test", bootcamp.managerEmail)
        // 교과목은 교과편성 화면 순서가 아니라 수업 순서로 적는다.
        assertTrue(
            bootcamp.content.endsWith(
                """
                [훈련목표]
                - 웹 서비스를 구현할 수 있다.
                - 배포할 수 있다.

                [교과목]
                ■ 자바 (139 시간)
                - 기초 문법
                ■ 프로젝트 (200 시간)
                ① 주제1
                - 맛집 리뷰 서비스

                [훈련교재]
                - 쉽게 시작하는 쿠버네티스
                """.trimIndent(),
            ),
            bootcamp.content,
        )
        assertEquals("선수학습: 특별한 선수학습 없음", bootcamp.eligibilityAndSelectionProcess)
        assertEquals("■ 프로젝트 기반 학습", bootcamp.programFeatures)
        assertEquals("민*식 (전기정보공): 정보처리기사", bootcamp.instructorInfo)
        // 수업이 없는 주가 끼면 구간을 나눈다.
        assertEquals(
            listOf(Triple(1, 2, "자바"), Triple(2, 2, "프로젝트"), Triple(4, 4, "프로젝트")),
            bootcamp.curriculums.map { Triple(it.startWeek, it.endWeek, it.subtitle) },
        )
    }

    @Test
    fun `일학습병행 훈련과정은 학습기업을 회사로 하는 일학습병행 채용공고로 게시한다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313L01.do")))
            .andExpect(queryParam("srchTraStDt", "20260927"))
            .andRespond(
                xml(
                    """
                    <HRDNet><scn_cnt>1</scn_cnt><srchList><scn_list>
                      <trprId>ABF1</trprId><trprDegr>2</trprDegr><trainstCstId>ORG</trainstCstId>
                      <title>2026년_공동훈련센터형_선박도장_L2_25V1_거제대학교_주식회사화인기업</title><subTitle>거제대학교</subTitle>
                      <titleLink>$WORK_STUDY/ABF1</titleLink><trainTarget>공동훈련센터형</trainTarget>
                      <traStartDate>2026-09-30</traStartDate><traEndDate>2027-09-29</traEndDate>
                      <address>경남 거제시</address><trngAreaCd>48310</trngAreaCd><yardMan>0</yardMan><telNo>055-631-9579</telNo>
                    </scn_list></srchList></HRDNet>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313D01.do")))
            .andExpect(queryParam("srchTrprId", "ABF1"))
            .andExpect(queryParam("srchTrprDegr", "2"))
            .andRespond(
                xml("<HRDNet><inst_base_info><inoNm>거제대학교</inoNm><ncsNm>선박도장</ncsNm></inst_base_info></HRDNet>"),
            )
        // 학습기업 이름과 훈련목적, 현장 교육훈련 편성은 과정 상세 화면에서 읽는다.
        server.expect(requestTo("$WORK_STUDY/ABF1")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(WORK24_WORK_STUDY_PAGE_HTML, MediaType.TEXT_HTML))

        // when
        val result = collector().collect(Work24CollectionTarget.WORK_STUDY_COURSES, NOW)

        // then
        server.verify()
        assertEquals(1, result.appendedCount)
        assertTrue(appendedBootcamps.isEmpty())
        val job = appendedJobs.single()
        // 과정명의 표기(주식회사화인기업)가 아니라 화면의 학습기업 이름을 쓴다.
        assertEquals("(주)화인기업", job.companyName)
        assertEquals(
            """
            훈련 분야: 선박도장
            훈련 유형: 공동훈련센터형
            훈련 기간: 2026-09-30 ~ 2027-09-29
            현장 교육훈련(OJT): 12개월 / 360일 / 총600시간
            사업장 외 교육훈련(Off-JT): 12개월 / 360일 / 총200시간

            [훈련목적]
            선박도장에 필요한 능력을 함양한다.

            [현장 교육훈련(OJT) 편성]
            - 선박도장실무 1: 선박도장 터치업 도장 (필수, 80 시간)
            - SQL활용 (선택, 40 시간)
            """.trimIndent(),
            job.responsibilities,
        )
        assertNull(job.qualifications)
        assertEquals("2026년_공동훈련센터형_선박도장_L2_25V1_거제대학교_주식회사화인기업", job.title)
        assertEquals(JobEmploymentType.WORK_STUDY, job.employmentType)
        assertEquals(JobExperienceType.IRRELEVANT, job.experienceType)
        assertEquals(Region.GYEONGNAM, job.region)
        assertEquals(JobRecruitmentType.PERIOD, job.recruitmentType)
        assertEquals(NOW, job.recruitmentStartAt)
        assertEquals(LocalDateTime.of(2026, 9, 30, 23, 59, 59), job.recruitmentEndAt)
        assertNull(job.recruitmentHeadcount)
        assertEquals("$WORK_STUDY/ABF1", job.sourceUrl)
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
        assertEquals(ContentSource.WORK24, job.source)
        assertEquals("ABF1-2", job.externalId)
    }

    @Test
    fun `일학습병행 상세 화면을 읽지 못하면 과정명의 마지막 부분을 회사명으로 쓴다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313L01.do")))
            .andRespond(
                xml(
                    "<HRDNet><scn_cnt>1</scn_cnt><srchList><scn_list><trprId>ABF1</trprId><trprDegr>1</trprDegr>" +
                        "<trainstCstId>ORG</trainstCstId><title>2026년_선박도장_거제대학교_주식회사화인기업</title>" +
                        "<subTitle>거제대학교</subTitle><titleLink>$WORK_STUDY/ABF1</titleLink></scn_list></srchList></HRDNet>",
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo313D01.do"))).andRespond(xml("<HRDNet><a>1</a></HRDNet>"))
        server.expect(requestTo("$WORK_STUDY/ABF1")).andRespond(withServerError())

        // when
        val result = collector().collect(Work24CollectionTarget.WORK_STUDY_COURSES, NOW)

        // then
        server.verify()
        assertEquals(1, result.appendedCount)
        assertEquals("주식회사화인기업", appendedJobs.single().companyName)
    }

    @Test
    fun `과정 상세 화면을 읽지 못해도 Open API 값만으로 부트캠프를 등록한다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andRespond(xml(trainingPage(total = 1, ids = listOf("C1"))))
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L02.do")))
            .andRespond(xml("<HRDNet><inst_base_info><inoNm>오공고 아카데미</inoNm></inst_base_info></HRDNet>"))
        server.expect(requestTo("$HRD/C1")).andRespond(withServerError())

        // when
        val result = collector().collect(Work24CollectionTarget.K_DIGITAL_TRAINING_COURSES, NOW)

        // then
        server.verify()
        assertEquals(1, result.appendedCount)
        assertEquals(0, result.failedCount)
        val bootcamp = appendedBootcamps.single()
        assertEquals("목록 과정명", bootcamp.title)
        assertNull(bootcamp.eligibilityAndSelectionProcess)
        assertTrue(bootcamp.images.isEmpty())
        assertTrue(bootcamp.curriculums.isEmpty())
        assertTrue(bootcamp.partners.isEmpty())
        assertEquals(false, bootcamp.enterpriseLinked)
    }

    @Test
    fun `페이지 파라미터를 무시하고 같은 목록을 되풀이하면 멈춘다`() {
        // 전체 건수가 없고 페이지가 가득 차 있어 다음 페이지를 요청하게 된다.
        val samePage = "<wantedRoot>" +
            (1..100).joinToString("") { "<wanted><wantedAuthNo>K$it</wantedAuthNo><jobsCd>133201</jobsCd><wantedInfoUrl>$WORKNET/K$it</wantedInfoUrl></wanted>" } +
            "</wantedRoot>"
        (1..100).forEach { Mockito.`when`(jobReader.existsByExternalId(ContentSource.WORK24, "K$it")).thenReturn(true) }
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
                      <wanted><wantedAuthNo>K1</wantedAuthNo><jobsCd>133201</jobsCd></wanted>
                      <wanted><wantedAuthNo>K2</wantedAuthNo><jobsCd>133201</jobsCd><company>회사</company><title>제목</title><wantedInfoUrl>$WORKNET/K2</wantedInfoUrl></wanted>
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
                        (1..20).joinToString("") { "<wanted><wantedAuthNo>K$it</wantedAuthNo><jobsCd>133201</jobsCd><wantedInfoUrl>$WORKNET/K$it</wantedInfoUrl></wanted>" } +
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
    fun `시급·일급 공고와 직군·직무로 분류할 수 없는 직종의 공고는 상세를 부르지 않고 제외한다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>4</total>
                      <wanted><wantedAuthNo>K1</wantedAuthNo><salTpNm>시급</salTpNm><wantedInfoUrl>$WORKNET/K1</wantedInfoUrl></wanted>
                      <wanted><wantedAuthNo>K2</wantedAuthNo><salTpNm>일급</salTpNm><wantedInfoUrl>$WORKNET/K2</wantedInfoUrl></wanted>
                      <wanted><wantedAuthNo>K3</wantedAuthNo><salTpNm>월급</salTpNm><jobsCd>133201</jobsCd><company>회사</company><title>제목</title><wantedInfoUrl>$WORKNET/K3</wantedInfoUrl></wanted>
                      <wanted><wantedAuthNo>K4</wantedAuthNo><salTpNm>월급</salTpNm><jobsCd>999999</jobsCd><wantedInfoUrl>$WORKNET/K4</wantedInfoUrl></wanted>
                    </wantedRoot>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
            .andExpect(queryParam("wantedAuthNo", "K3"))
            .andRespond(xml("<wantedDtl/>"))

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        server.verify()
        assertEquals(3, result.excludedCount)
        assertEquals(listOf("K3"), appendedJobs.map { it.externalId })
    }

    @Test
    fun `임시로 정한 직군 밖 직종의 공고는 상세를 부르지 않고 제외한다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    """
                    <wantedRoot><total>2</total>
                      <wanted><wantedAuthNo>K1</wantedAuthNo><jobsCd>140100</jobsCd><wantedInfoUrl>$WORKNET/K1</wantedInfoUrl></wanted>
                      <wanted><wantedAuthNo>K2</wantedAuthNo><jobsCd>024102</jobsCd><company>회사</company><title>제목</title><wantedInfoUrl>$WORKNET/K2</wantedInfoUrl></wanted>
                    </wantedRoot>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
            .andExpect(queryParam("wantedAuthNo", "K2"))
            .andRespond(xml("<wantedDtl/>"))

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        server.verify()
        assertEquals(1, result.excludedCount)
        assertEquals(listOf(JobField.MARKETING_ADVERTISING), appendedJobs.map { it.jobField })
    }

    @Test
    fun `최소 경력이 3년보다 긴 공고는 상세를 보고 제외하고 3년까지는 최소 경력 연수를 넣어 게시한다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andRespond(
                xml(
                    "<wantedRoot><total>3</total>" +
                        listOf("K1", "K2", "K3").joinToString("") {
                            "<wanted><wantedAuthNo>$it</wantedAuthNo><jobsCd>133201</jobsCd><company>회사</company>" +
                                "<title>제목</title><wantedInfoUrl>$WORKNET/$it</wantedInfoUrl></wanted>"
                        } +
                        "</wantedRoot>",
                ),
            )
        listOf("경력 (최소3년) 우대", "경력 (최소5년) 필수", "경력 (6개월 이상) 우대").forEach { career ->
            server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210D01.do")))
                .andRespond(xml("<wantedDtl><wantedInfo><enterTpCd>E</enterTpCd><enterTpNm>$career</enterTpNm></wantedInfo></wantedDtl>"))
        }

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, NOW)

        server.verify()
        assertEquals(1, result.excludedCount)
        assertEquals(listOf("K1", "K3"), appendedJobs.map { it.externalId })
        assertEquals(listOf(3, null), appendedJobs.map { it.experienceMinYears })
    }

    private fun collector(): Work24Collector {
        val properties = Work24Properties(
            baseUrl = BASE_URL,
            recruitmentAuthKey = "recruitment-key",
            tomorrowLearningCardAuthKey = "training-key",
            workStudyAuthKey = "work-study-key",
        )
        return Work24Collector(
            Work24Client(restClientBuilder.build(), properties, ObjectMapper()),
            jobReader,
            jobAppender,
            bootcampReader,
            bootcampAppender,
            Work24CoursePageReader(restClientBuilder.build()),
            Work24InstitutionImageImporter(
                restClientBuilder.build(),
                // S3 대신 저장 키로 공개 주소를 만들어 돌려준다.
                Mockito.mock(S3ObjectClient::class.java) { invocation ->
                    if (invocation.method.name == "isConfigured") true else "$CDN/${invocation.getArgument<String>(0)}"
                },
            ),
        )
    }

    private fun form(vararg fields: Pair<String, String>) =
        LinkedMultiValueMap<String, String>().apply { fields.forEach { (name, value) -> add(name, value) } }

    private fun trainingPage(total: Int, ids: List<String>, institutionLink: Boolean = false): String =
        "<HRDNet><scn_cnt>$total</scn_cnt><srchList>" +
            ids.joinToString("") { id ->
                "<scn_list><trprId>$id</trprId><trprDegr>1</trprDegr><trainstCstId>ORG</trainstCstId>" +
                    "<title>목록 과정명</title><subTitle>목록 기관명</subTitle><titleLink>$HRD/$id</titleLink>" +
                    "<trainTarget>국가기간전략산업직종</trainTarget>" +
                    (if (institutionLink) "<subTitleLink>$INSTITUTION</subTitleLink>" else "") +
                    "<traStartDate>2026-10-05</traStartDate><traEndDate>2027-03-31</traEndDate></scn_list>"
            } +
            "</srchList></HRDNet>"

    private fun xml(body: String) = withSuccess(body, MediaType.APPLICATION_XML)

    private companion object {
        const val BASE_URL = "https://work24.test/cm/openApi/call"
        const val WORKNET = "https://www.work24.go.kr/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo="
        const val WORK24_SITE = "https://www.work24.go.kr"
        const val CDN = "https://cdn.ogonggo.test"
        const val INSTITUTION = "$WORK24_SITE/hr/a/a/3200/selectTrainInstitution.do?trainstCstmrId=ORG"
        const val HRD = "$WORK24_SITE/hr/a/a/3100/selectTracseDetl.do?tracseId="
        const val WORK_STUDY = "$WORK24_SITE/hr/a/a/3100/selectJobAndStudyTracseDetail.do?tracseId="
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 27, 4, 0)
    }
}
