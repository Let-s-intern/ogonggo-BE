package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.userapi.advertisementcontract.presentation.request.CreateAdvertisementContractRequest
import com.ogonggo.userapi.advertisementcontract.presentation.response.CreateAdvertisementContractResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "광고 신청")
interface AdvertisementContractApi {

    @Operation(
        operationId = "createAdvertisementContract",
        summary = "광고 신청 등록",
        description = """
            광고 신청 정보를 저장하고 새 계약 ID를 반환합니다. 신청은 APPLICATION_FORM·REQUESTED로 저장합니다.
            인증: 비회원 허용, Bearer 선택. 로그인 없이 보낼 수 있으며, 유효한 액세스 토큰이 있으면 회원 ID를 연결합니다. 토큰이 없거나 유효하지 않거나
            토큰의 회원 행이 없으면 비회원 신청으로 접수합니다.

            categories에는 희망 카테고리만 저장합니다. 실제 상품 연결, 공고 생성, 노출, 결제, 구좌 예약은 만들지 않습니다.
            입력 원문은 보존하며 scheduleRequest와 advertisementRequest만 공백이면 저장 시 null이 됩니다.
            budgetAmount(예정 예산, 원 단위 정수)는 선택이며 보내지 않으면 null로 저장합니다.

            businessNumberType과 businessNumber는 선택입니다. 보내면 기업 회원가입과 같은 규칙으로 검사합니다. 사업자 번호는 하이픈 없는
            숫자로, 사업자등록번호는 10자리, 법인등록번호는 13자리입니다. 해외 기업(OVERSEAS)은 번호를 보내지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "신청 접수 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수값이 없거나 형식·길이가 맞지 않습니다. 사업자 번호를 번호 종류 없이 보내거나 종류와 자릿수가 맞지 않아도 400입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun createContract(
        @Parameter(hidden = true)
        userId: Long?,
        @Valid
        request: CreateAdvertisementContractRequest,
    ): ResponseEntity<SuccessResponse<CreateAdvertisementContractResponse>>
}
