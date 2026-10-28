package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.presentation.request.CompanySignInRequest
import com.ogonggo.userapi.auth.presentation.request.CompanySignUpRequest
import com.ogonggo.userapi.auth.presentation.response.AuthTokenResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "기업 인증")
interface CompanyAuthApi {

    @Operation(
        operationId = "signUpCompany",
        summary = "기업 회원가입",
        description = """
            렛츠커리어를 거치지 않고 오공고 계정을 만듭니다. 승인 절차가 없어 가입 즉시 세션을 발급합니다.

            사업자 번호·기업 형태·직원 수·산업·주소·홈페이지·담당자 직책·담당 업무·연락처·추천인은 모두 선택입니다.
            이 값을 받기 전에 배포된 가입 화면도 같은 API를 부르기 때문이며, 어느 값이 필수인지는 가입 화면이 정합니다.
            보낸 값은 형식을 검사합니다. 사업자 번호는 하이픈 없는 숫자로, businessNumberType이
            BUSINESS_REGISTRATION이면 10자리, CORPORATE_REGISTRATION이면 13자리이고, OVERSEAS면 보내지 않습니다.
            번호 종류 없이 번호만 보내면 거부합니다. 번호의 진위와 이메일 소유는 확인하지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "가입 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 입력 형식이 올바르지 않거나 사업자 번호가 번호 종류와 맞지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "EMAIL_ALREADY_EXISTS: 이미 사용 중인 이메일입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun signUp(
        @Valid
        request: CompanySignUpRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>>

    @Operation(operationId = "signInCompany", summary = "기업 로그인")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "INVALID_COMPANY_CREDENTIALS: 이메일 또는 비밀번호가 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun signIn(
        @Valid
        request: CompanySignInRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>>
}
