package com.ogonggo.userapi.contentevent.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.contentevent.presentation.request.CreateContentEventsRequest
import com.ogonggo.userapi.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "콘텐츠 이벤트")
interface UserContentEventApi {

    @Operation(
        operationId = "createContentEvents",
        summary = "콘텐츠 이벤트 수집",
        description = """
            공고·부트캠프·취준고민·모집글 상세의 조회(VIEW)·CTA 클릭(CTA_CLICK)·스크롤(SCROLL)·공유(SHARE)와 광고의 노출(IMPRESSION)·클릭(AD_CLICK)을
            한 번에 1~20건 묶어 보냅니다. 광고 성과 리포트가 이것을 셉니다.

            로그인 없이 보낼 수 있습니다. 액세스 토큰을 보내면 회원을 함께 기록합니다.
            Content-Type은 application/json과 text/plain(같은 JSON) 둘 다 받습니다. 페이지를 떠날 때 navigator.sendBeacon으로 보내기 위해서입니다.

            저장할 수 없는 건은 오류 없이 그 건만 버리고 나머지를 저장합니다: 종류에 필요한 대상·노출이 없음, 스크롤이 아닌데 scrollPercent·durationMs가 있음,
            대상이 없거나 게시 중이 아님, 노출이 없거나 지워짐, 광고 공고 노출의 대상과 이벤트의 대상이 다름.
            시각은 서버가 받은 시각(KST)으로 적습니다. 같은 방문자의 재조회도 모두 저장하며 1분 안 재조회를 한 번으로 세는 것은 집계가 합니다.

            저장한 조회(VIEW)는 대상의 누적 조회수(viewCount)도 1 올립니다. 같은 방문자(회원이면 회원)가 같은 대상을 직전 조회로부터 1분 안에
            다시 연 것은 올리지 않습니다. 상세 조회 GET은 조회수를 올리지 않으므로 상세를 열 때 이 이벤트를 보내야 합니다.

            알려진 봇·크롤러 User-Agent, User-Agent가 없는 요청, 같은 방문자(분당 60회)·같은 IP(분당 600회)의 상한을 넘은 요청은
            아무것도 저장하지 않지만 응답은 똑같이 202입니다.

            응답은 본문 없는 202입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "202", description = "접수"),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: visitorId가 없거나 36자를 넘거나, events가 비었거나 20건을 넘거나, 값의 형식·범위가 맞지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun createContentEvents(
        @Parameter(hidden = true)
        userId: Long?,
        @Valid
        request: CreateContentEventsRequest,
        @Parameter(hidden = true)
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void>
}
