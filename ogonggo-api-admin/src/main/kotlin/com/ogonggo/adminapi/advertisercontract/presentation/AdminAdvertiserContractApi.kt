package com.ogonggo.adminapi.advertisercontract.presentation

import com.ogonggo.adminapi.advertisercontract.presentation.request.CreateAdminAdvertiserContractRequest
import com.ogonggo.adminapi.advertisercontract.presentation.request.UpdateAdminAdvertiserContractRequest
import com.ogonggo.adminapi.advertisercontract.presentation.response.AdminAdvertiserContractIdResponse
import com.ogonggo.adminapi.advertisercontract.presentation.response.AdminAdvertiserContractResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

private const val ADVERTISER_NOT_FOUND_DESCRIPTION = "ADVERTISER_NOT_FOUND: 광고주를 찾을 수 없습니다."
private const val CONTRACT_NOT_FOUND_DESCRIPTION = "ADVERTISER_CONTRACT_NOT_FOUND: 계약을 찾을 수 없습니다."
private const val CONTRACT_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: endAt: 광고 끝은 시작보다 뒤여야 합니다. / " +
        "amount: 0 이상의 정수(원)로 보내 주세요. / vatIncluded: 참·거짓 값을 보내 주세요. / " +
        "products: ... 가운데 겹치지 않게 보내 주세요. / name: 50자까지 보낼 수 있습니다. / memo: 500자까지 보낼 수 있습니다."

@Tag(name = "관리자 광고 계약")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertiserContractApi {

    @Operation(
        operationId = "listAdvertiserContracts",
        summary = "광고주의 광고 계약 목록 조회",
        description = "startAt이 늦은 것이 먼저이고 같으면 id가 큰 것이 먼저입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = ADVERTISER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getContracts(
        @Positive advertiserId: Long,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserContractResponse>>>

    @Operation(operationId = "createAdvertiserContract", summary = "광고 계약 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CONTRACT_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = ADVERTISER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createContract(
        @Positive advertiserId: Long,
        @Valid request: CreateAdminAdvertiserContractRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractResponse>>

    @Operation(
        operationId = "updateAdvertiserContract",
        summary = "광고 계약 수정",
        description = "보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CONTRACT_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CONTRACT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateContract(
        @Positive contractId: Long,
        @Valid request: UpdateAdminAdvertiserContractRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractResponse>>

    @Operation(
        operationId = "deleteAdvertiserContract",
        summary = "광고 계약 삭제",
        description = "계약만 지웁니다. 이어 둔 배너·광고 공고는 지우지 않고 계약 연결만 끊습니다. 이미 지운 계약을 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = CONTRACT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteContract(
        @Positive contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserContractIdResponse>>
}
