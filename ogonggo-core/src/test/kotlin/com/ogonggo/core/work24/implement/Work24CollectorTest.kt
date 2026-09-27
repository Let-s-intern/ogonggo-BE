package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.implement.dto.Work24ItemAppendDto
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDate

class Work24CollectorTest {

    private val restClientBuilder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    /** 저장소 대신 받은 항목을 기록하고 모두 새 항목으로 본다. */
    private val appended = mutableListOf<Pair<Work24Api, List<Work24ItemAppendDto>>>()
    private val appender = Mockito.mock(Work24ItemAppender::class.java) { invocation ->
        val items = invocation.getArgument<List<Work24ItemAppendDto>>(1)
        appended += invocation.getArgument<Work24Api>(0) to items
        items.size
    }

    @Test
    fun `전체 건수만큼 받을 때까지 페이지를 넘기고 날짜 조건과 페이지 크기를 보낸다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("pageNum", "1"))
            .andExpect(queryParam("pageSize", "100"))
            .andExpect(queryParam("srchTraStDt", "20260927"))
            .andExpect(queryParam("srchTraEndDt", "20261226"))
            .andRespond(xml(trainingPage(total = 101, courses = (1..100).map { "C$it" to "1" })))
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("pageNum", "2"))
            // 마지막 페이지에 항목이 하나뿐이면 배열이 아니라 객체로 온다.
            .andRespond(xml(trainingPage(total = 101, courses = listOf("C101" to "2"))))

        // when
        val result = collector().collect(Work24CollectionTarget.TOMORROW_LEARNING_CARD_COURSES, TODAY)

        // then
        server.verify()
        assertEquals(2, result.pageCount)
        assertEquals(101, result.fetchedCount)
        assertEquals(101, result.appendedCount)
        assertEquals("C1-1", appended.first().second.first().externalId)
        assertEquals("C101-2", appended.last().second.single().externalId)
    }

    @Test
    fun `페이지 파라미터를 무시하고 같은 목록을 되풀이하면 멈춘다`() {
        // 전체 건수가 없고 페이지가 가득 차 있어 다음 페이지를 요청하게 된다.
        val samePage = "<ilmoaJobsList>" +
            (1..100).joinToString("") { "<ilmoaJob><bsnsId>B$it</bsnsId></ilmoaJob>" } +
            "</ilmoaJobsList>"
        repeat(2) {
            server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo211L02.do"))).andRespond(xml(samePage))
        }

        val result = collector().collect(Work24CollectionTarget.GOVERNMENT_JOB_PROGRAMS, TODAY)

        server.verify()
        assertEquals(2, result.pageCount)
        assertEquals(100, appended.single().second.size)
    }

    @Test
    fun `식별 필드가 없는 목록은 항목 내용으로 구별하고 식별값이 빈 항목은 건너뛴다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo217L01.do")))
            .andRespond(
                xml(
                    """
                    <empPgmSchdInviteList><total>2</total>
                      <empPgmSchdInvite><pgmNm>면접 특강</pgmNm></empPgmSchdInvite>
                      <empPgmSchdInvite><pgmNm>이력서 클리닉</pgmNm></empPgmSchdInvite>
                    </empPgmSchdInviteList>
                    """.trimIndent(),
                ),
            )
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo216L31.do")))
            .andRespond(
                xml(
                    """
                    <smallGiantsList><total>2</total>
                      <smallGiant><busiNo>123</busiNo></smallGiant>
                      <smallGiant><busiNo></busiNo></smallGiant>
                    </smallGiantsList>
                    """.trimIndent(),
                ),
            )

        val collector = collector()
        collector.collect(Work24CollectionTarget.JOB_SEEKER_PROGRAMS, TODAY)
        collector.collect(Work24CollectionTarget.YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES, TODAY)

        val programs = appended[0].second
        assertEquals(2, programs.map { it.externalId }.distinct().size)
        assertTrue(programs.all { it.externalId.length == 64 })
        assertEquals(listOf("123"), appended[1].second.map { it.externalId })
    }

    @Test
    fun `항목이 없으면 저장하지 않고 끝낸다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo210L01.do")))
            .andExpect(queryParam("callTp", "L"))
            .andExpect(queryParam("regDate", "D-3"))
            .andRespond(xml("<wantedRoot><total>0</total></wantedRoot>"))

        val result = collector().collect(Work24CollectionTarget.RECRUITMENTS, TODAY)

        assertEquals(0, result.fetchedCount)
        assertTrue(appended.isEmpty())
    }

    private fun collector(): Work24Collector {
        val properties = Work24Properties(
            baseUrl = BASE_URL,
            recruitmentAuthKey = "recruitment-key",
            tomorrowLearningCardAuthKey = "training-key",
            governmentJobAuthKey = "government-key",
            jobSeekerProgramAuthKey = "program-key",
            smallGiantCompanyAuthKey = "small-giant-key",
        )
        val objectMapper = ObjectMapper()
        return Work24Collector(Work24Client(restClientBuilder.build(), properties, objectMapper), appender, objectMapper)
    }

    private fun trainingPage(total: Int, courses: List<Pair<String, String>>): String =
        "<HRDNet><scn_cnt>$total</scn_cnt><srchList>" +
            courses.joinToString("") { (id, degree) ->
                "<scn_list><trprId>$id</trprId><trprDegr>$degree</trprDegr></scn_list>"
            } +
            "</srchList></HRDNet>"

    private fun xml(body: String) = withSuccess(body, MediaType.APPLICATION_XML)

    private companion object {
        const val BASE_URL = "https://work24.test/cm/openApi/call"
        val TODAY: LocalDate = LocalDate.of(2026, 9, 27)
    }
}
