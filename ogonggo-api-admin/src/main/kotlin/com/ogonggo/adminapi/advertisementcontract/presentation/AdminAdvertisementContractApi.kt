package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.presentation.request.AddAdminAdvertisementContractProductRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.request.ChangeAdminAdvertisementContractPaymentAmountRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.request.ChangeAdminAdvertisementContractStatusRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementContractDetailResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementContractSummaryResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminBusinessAccountIssueResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.CreateAdminAdvertisementContractProductResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import java.time.LocalDate

private const val CONTRACT_NOT_FOUND = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다."
private const val CONTRACT_PRODUCT_NOT_FOUND = "ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND: 계약에 붙인 광고 상품을 찾을 수 없습니다."
private const val CONTRACT_FOLLOWS_REVIEW = "ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW: 비즈니스 공고 계약의 상태는 공고 검수로만 바뀝니다."
private const val BUSINESS_ACCOUNT_NOT_ISSUABLE =
    "BUSINESS_ACCOUNT_NOT_ISSUABLE: 비회원이 광고 신청 폼으로 낸 계약에만 비즈니스 계정을 발급할 수 있습니다."
private const val BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE =
    "BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE: 신청 이메일로 가입한 비즈니스 회원이 정지·탈퇴 상태라 연결할 수 없습니다."
private const val EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS: 같은 이메일로 동시에 가입했습니다. 다시 요청하면 그 회원에 연결합니다."
private const val PRODUCT_NOT_FOUND = "ADVERTISEMENT_PRODUCT_NOT_FOUND: 광고 상품을 찾을 수 없습니다."

@Tag(name = "관리자 광고 계약")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementContractApi {

    @Operation(
        operationId = "listAdvertisementContracts",
        summary = "광고 계약 목록 조회",
        description = """
            광고 계약을 ID 내림차순으로 조회합니다. source, status, promotionTarget, category, companyName, createdFrom·createdTo 필터를 지원합니다.
            서로 다른 필터는 AND로 결합하고, status를 여러 번 보내면 그중 하나에 해당하는 계약을 찾습니다.
            companyName은 앞뒤 공백을 제거한 뒤 대소문자를 가리지 않는 부분 검색을 합니다.
            createdFrom과 createdTo는 생성일 기준이며 양 끝 날짜를 포함합니다. page는 1부터 시작하고 기본 1, size 기본 20·최대 100입니다.
            COMPANY_POSTING 계약도 포함하며, categories는 신청 희망 카테고리입니다. 연결 상품은 상세 조회에서 확인합니다.
            budgetAmount는 신청 폼에 적은 예정 예산(원)이며, 적지 않은 신청과 COMPANY_POSTING 계약은 null입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위, 필터 값, 회사명 길이 또는 기간이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getContracts(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        source: AdvertisementContractSource?,
        @Parameter(description = "여러 번 전달하면 해당 상태 중 하나인 계약을 조회합니다.")
        statuses: List<AdvertisementContractStatus>?,
        promotionTarget: AdvertisementContractPromotionTarget?,
        category: AdvertisementProductCategory?,
        @Parameter(description = "앞뒤 공백을 제거하는 대소문자 무시 부분 검색입니다.")
        @Size(max = 100) companyName: String?,
        @Parameter(description = "생성일 시작(포함)") createdFrom: LocalDate?,
        @Parameter(description = "생성일 끝(포함)") createdTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementContractSummaryResponse>>>

    @Operation(
        operationId = "getAdvertisementContract",
        summary = "광고 계약 상세 조회",
        description = "계약 신청 원문, 희망 카테고리와 제거되지 않은 연결 상품을 반환합니다. 상품명과 category는 조회 시점의 현재 상품 값이며 판매 당시 스냅샷이 아닙니다. 삭제된 상품도 연결과 현재 상품명, 삭제 여부를 표시합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: contractId는 양수여야 하며 Path ID 형식이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getContract(
        @Positive contractId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementContractDetailResponse>>

    @Operation(
        operationId = "updateAdvertisementContractStatus",
        summary = "광고 계약 상태 변경",
        description = "APPLICATION_FORM 계약의 상태를 변경합니다. COMPANY_POSTING 계약은 기존 공고 검수 경로에서만 상태가 바뀝니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: status 누락 또는 enum 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CONTRACT_NOT_FOUND,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = CONTRACT_FOLLOWS_REVIEW,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun changeStatus(
        @Positive contractId: Long,
        @Valid request: ChangeAdminAdvertisementContractStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "updateAdvertisementContractPaymentAmount",
        summary = "계약 협의 금액 입력·해제",
        description = "결제 실행이나 입금 확인 없이 협의 금액만 저장합니다. paymentAmount 키는 필수이며 null은 금액 해제, 0은 0원입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: paymentAmount 키가 없거나 0 미만 또는 정수 범위 밖입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CONTRACT_NOT_FOUND,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun changePaymentAmount(
        @Positive contractId: Long,
        @Valid request: ChangeAdminAdvertisementContractPaymentAmountRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "createAdvertisementContractProduct",
        summary = "계약에 상품 추가",
        description = "미삭제 상품을 계약에 연결합니다. 같은 상품의 중복 연결을 허용하며 노출 설정은 만들지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "연결 생성", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: contractId 또는 productId는 양수여야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "$CONTRACT_NOT_FOUND $PRODUCT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun addProduct(
        @Positive contractId: Long,
        @Valid request: AddAdminAdvertisementContractProductRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementContractProductResponse>>

    @Operation(
        operationId = "deleteAdvertisementContractProduct",
        summary = "계약에서 상품 제거",
        description = "해당 계약에 속한 살아 있는 연결 행만 소프트 삭제합니다. 기존 광고 노출은 변경하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "제거 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: contractId와 contractProductId는 양수여야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "$CONTRACT_NOT_FOUND $CONTRACT_PRODUCT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun removeProduct(
        @Positive contractId: Long,
        @Positive contractProductId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "issueAdvertisementContractBusinessAccount",
        summary = "비즈니스 계정 발급",
        description = "비회원 신청 폼 계약의 신청 이메일로 비즈니스 계정을 발급합니다. " +
            "그 이메일의 비즈니스 회원이 없으면 신청서의 기업명·담당자·연락처·사업자 번호로 만들고 임시 비밀번호 메일을 발송 대기열에 넣습니다(created=true). " +
            "운영자가 고객에게 전달할 수 있도록 로그인 이메일과 임시 비밀번호를 응답에 담습니다. 임시 비밀번호는 이 응답에서만 볼 수 있습니다. " +
            "있으면 새로 만들지 않고 비밀번호도 바꾸지 않습니다(created=false, temporaryPassword=null). " +
            "어느 쪽이든 그 이메일로 낸 비회원 신청을 모두 그 회원에 연결하고 연결한 수를 돌려줍니다. 응답은 메일 적재까지이며 실제 발송 결과가 아닙니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "발급 또는 연결 완료", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: contractId는 양수여야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = CONTRACT_NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(
                responseCode = "409",
                description = "$BUSINESS_ACCOUNT_NOT_ISSUABLE $BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE $EMAIL_ALREADY_EXISTS",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun issueBusinessAccount(@Positive contractId: Long): ResponseEntity<SuccessResponse<AdminBusinessAccountIssueResponse>>
}
