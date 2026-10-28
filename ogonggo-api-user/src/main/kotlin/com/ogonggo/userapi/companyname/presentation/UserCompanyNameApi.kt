package com.ogonggo.userapi.companyname.presentation

import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "회사명")
interface UserCompanyNameApi {

    @Operation(
        operationId = "listPublicCompanyNames",
        summary = "회사명 검색",
        description = """
            오공고에 있는 회사명 가운데 keyword를 포함하는 이름을 최대 10개 반환합니다. 페이지 정보는 없습니다.
            광고 문의 폼의 회사명 칸이 이미 있는 이름을 고르게 권하는 데 씁니다.

            삭제되지 않은 채용공고(마감·비노출 포함)와 부트캠프(마감 포함)의 회사명, 기업 회원의 기관명을 합쳐 찾습니다.
            앞뒤 공백만 다듬어 같은 이름을 하나로 합치고 표기는 바꾸지 않습니다.
            keyword로 시작하는 이름이 먼저, 그다음 포함하는 이름이며 같은 묶음 안에서는 짧은 이름이 먼저입니다.
            대소문자를 가리지 않습니다.

            로그인 없이 호출합니다. keyword는 앞뒤 공백을 지우고 찾으며, 보내지 않거나 비면 빈 목록입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: keyword가 50자를 넘습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getCompanyNames(
        @Parameter(description = "회사명 검색어")
        @Size(max = 50)
        keyword: String?,
    ): ResponseEntity<SuccessResponse<List<String>>>
}
