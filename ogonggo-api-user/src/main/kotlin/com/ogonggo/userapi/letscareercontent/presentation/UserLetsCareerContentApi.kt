package com.ogonggo.userapi.letscareercontent.presentation

import com.ogonggo.userapi.letscareercontent.presentation.response.UserRecommendedLetsCareerContentResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "렛츠커리어 콘텐츠 추천")
interface UserLetsCareerContentApi {

    @Operation(
        operationId = "listPublicJobRecommendedLetsCareerContents",
        summary = "채용공고에 맞는 렛츠커리어 콘텐츠 추천",
        description = """
            게시 중인 채용공고에 맞는 렛츠커리어 프로그램·무료 자료집·블로그를 섞어 최대 3개 반환합니다. 페이지 정보는 없습니다.

            공고의 직군·직무와 공고가 요구하는 제출 서류·전형(자기소개서, 포트폴리오, 면접, 인적성 등)으로 고르므로
            공고마다 결과가 다릅니다. 같은 공고는 다시 열어도 같은 결과입니다.

            로그인 없이 조회할 수 있습니다. 맞는 콘텐츠가 없으면 빈 배열이며, 이때는 추천 구역을 숨깁니다.
            카드를 누르면 `url`(렛츠커리어 웹 상세)로 이동합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 공고가 없거나 게시 중이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getRecommendedContents(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<List<UserRecommendedLetsCareerContentResponse>>>
}
