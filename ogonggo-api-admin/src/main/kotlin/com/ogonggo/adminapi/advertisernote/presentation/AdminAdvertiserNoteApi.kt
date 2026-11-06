package com.ogonggo.adminapi.advertisernote.presentation

import com.ogonggo.adminapi.advertisernote.presentation.request.CreateAdminAdvertiserNoteRequest
import com.ogonggo.adminapi.advertisernote.presentation.request.UpdateAdminAdvertiserNoteRequest
import com.ogonggo.adminapi.advertisernote.presentation.response.AdminAdvertiserNoteIdResponse
import com.ogonggo.adminapi.advertisernote.presentation.response.AdminAdvertiserNoteResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
private const val NOTE_NOT_FOUND_DESCRIPTION = "ADVERTISER_NOTE_NOT_FOUND: 메모를 찾을 수 없습니다."
private const val NOTE_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: title: 1~100자로 보내 주세요. / content: 1~2000자로 보내 주세요."

@Tag(name = "관리자 광고주 메모")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertiserNoteApi {

    @Operation(
        operationId = "listAdvertiserNotes",
        summary = "광고주 메모 목록 조회",
        description = "createdAt이 늦은 것이 먼저입니다.",
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
    fun getNotes(
        @Positive advertiserId: Long,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserNoteResponse>>>

    @Operation(
        operationId = "createAdvertiserNote",
        summary = "광고주 메모 등록",
        description = "쓴 사람(authorName)은 관리자 토큰의 회원으로 정하며 본문으로 받지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = NOTE_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = ADVERTISER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createNote(
        @Positive advertiserId: Long,
        @Parameter(hidden = true) adminUserId: Long,
        @Valid request: CreateAdminAdvertiserNoteRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteResponse>>

    @Operation(operationId = "updateAdvertiserNote", summary = "광고주 메모 수정", description = "보낸 칸만 바꿉니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = NOTE_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = NOTE_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateNote(
        @Positive noteId: Long,
        @Valid request: UpdateAdminAdvertiserNoteRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteResponse>>

    @Operation(
        operationId = "deleteAdvertiserNote",
        summary = "광고주 메모 삭제",
        description = "이미 지운 메모를 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = NOTE_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteNote(
        @Positive noteId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteIdResponse>>
}
