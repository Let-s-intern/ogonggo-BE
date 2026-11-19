package com.ogonggo.userapi.contentevent.presentation

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.contentevent.business.CollectContentEventCommand
import com.ogonggo.userapi.contentevent.business.CollectContentEventsCommand
import com.ogonggo.userapi.contentevent.business.UserContentEventService
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserContentEventController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserContentEventControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userContentEventService: UserContentEventService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 이벤트를 보내면 회원 없이 넘기고 본문 없는 202로 응답한다`() {
        // when & then
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isAccepted)
            .andExpect(content().string(""))

        Mockito.verify(userContentEventService).collect(null, COMMAND)
    }

    @Test
    fun `로그인하면 회원을 함께 넘긴다`() {
        // when & then
        mockMvc.perform(
            post(PATH)
                .with(authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY),
        )
            .andExpect(status().isAccepted)

        Mockito.verify(userContentEventService).collect(USER_ID, COMMAND)
    }

    /** sendBeacon은 다른 오리진에 text/plain으로만 보낼 수 있다. */
    @Test
    fun `text plain 본문도 JSON으로 읽는다`() {
        // when & then
        mockMvc.perform(post(PATH).contentType("text/plain;charset=UTF-8").content(BODY))
            .andExpect(status().isAccepted)

        Mockito.verify(userContentEventService).collect(null, COMMAND)
    }

    @Test
    fun `이벤트가 없거나 20건을 넘거나 방문자 식별자가 없으면 400으로 응답한다`() {
        val event = """{"type":"VIEW","targetType":"JOB","targetId":15818,"source":"LIST"}"""
        listOf(
            """{"visitorId":"$VISITOR_ID","events":[]}""" to "events",
            """{"visitorId":"$VISITOR_ID","events":[${List(21) { event }.joinToString(",")}]}""" to "events",
            """{"visitorId":" ","events":[$event]}""" to "visitorId",
            """{"visitorId":"${"a".repeat(37)}","events":[$event]}""" to "visitorId",
            """{"visitorId":"$VISITOR_ID","events":[{"type":"SCROLL","targetType":"JOB","targetId":1,"source":"LIST","scrollPercent":101}]}""" to "events[0].scrollPercent",
        ).forEach { (body, field) ->
            mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(userContentEventService)
    }

    private companion object {
        const val PATH = "/api/v1/events"
        const val USER_ID = 17L
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        val BODY = """
            {
              "visitorId": "$VISITOR_ID",
              "events": [
                { "type": "VIEW", "targetType": "JOB", "targetId": 15818, "exposureId": 31, "source": "AD_CARD" },
                { "type": "SCROLL", "targetType": "JOB", "targetId": 15818, "exposureId": null, "source": "LIST", "scrollPercent": 72, "durationMs": 48000 },
                { "type": "AD_CLICK", "targetType": null, "targetId": null, "exposureId": 12, "source": "BANNER" }
              ]
            }
        """.trimIndent()
        val COMMAND = CollectContentEventsCommand(
            visitorId = VISITOR_ID,
            events = listOf(
                CollectContentEventCommand(ContentEventType.VIEW, ContentEventTargetType.JOB, 15818L, 31L, ContentEventSource.AD_CARD, null, null),
                CollectContentEventCommand(ContentEventType.SCROLL, ContentEventTargetType.JOB, 15818L, null, ContentEventSource.LIST, 72, 48_000L),
                CollectContentEventCommand(ContentEventType.AD_CLICK, null, null, 12L, ContentEventSource.BANNER, null, null),
            ),
        )
    }
}
