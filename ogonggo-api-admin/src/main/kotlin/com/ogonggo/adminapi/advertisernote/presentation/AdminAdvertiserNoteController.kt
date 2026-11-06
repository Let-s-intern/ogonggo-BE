package com.ogonggo.adminapi.advertisernote.presentation

import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteService
import com.ogonggo.adminapi.advertisernote.presentation.request.CreateAdminAdvertiserNoteRequest
import com.ogonggo.adminapi.advertisernote.presentation.request.UpdateAdminAdvertiserNoteRequest
import com.ogonggo.adminapi.advertisernote.presentation.response.AdminAdvertiserNoteIdResponse
import com.ogonggo.adminapi.advertisernote.presentation.response.AdminAdvertiserNoteResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 목록·등록은 광고주 아래(`/advertisers/{advertiserId}/notes`), 수정·삭제는 메모 하나(`/advertiser-notes/{noteId}`)로 둔다. */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
class AdminAdvertiserNoteController(
    private val adminAdvertiserNoteService: AdminAdvertiserNoteService,
) : AdminAdvertiserNoteApi {

    @GetMapping("/advertisers/{advertiserId}/notes")
    override fun getNotes(
        @PathVariable("advertiserId") advertiserId: Long,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserNoteResponse>>> =
        SuccessResponse.ok(adminAdvertiserNoteService.getNotes(advertiserId).map(AdminAdvertiserNoteResponse::from))

    /** 등록이 커밋된 뒤의 메모를 다시 읽어 응답한다. */
    @PostMapping("/advertisers/{advertiserId}/notes")
    override fun createNote(
        @PathVariable("advertiserId") advertiserId: Long,
        @AuthenticationPrincipal adminUserId: Long,
        @RequestBody request: CreateAdminAdvertiserNoteRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteResponse>> {
        val noteId = adminAdvertiserNoteService.createNote(advertiserId, adminUserId, request.toCommand())
        return SuccessResponse.created(AdminAdvertiserNoteResponse.from(adminAdvertiserNoteService.getNote(noteId)))
    }

    /** 수정이 커밋된 뒤의 메모를 다시 읽어 응답한다. */
    @PatchMapping("/advertiser-notes/{noteId}")
    override fun updateNote(
        @PathVariable("noteId") noteId: Long,
        @RequestBody request: UpdateAdminAdvertiserNoteRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteResponse>> {
        adminAdvertiserNoteService.updateNote(noteId, request.toCommand())
        return SuccessResponse.ok(AdminAdvertiserNoteResponse.from(adminAdvertiserNoteService.getNote(noteId)))
    }

    @DeleteMapping("/advertiser-notes/{noteId}")
    override fun deleteNote(
        @PathVariable("noteId") noteId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserNoteIdResponse>> {
        adminAdvertiserNoteService.deleteNote(noteId)
        return SuccessResponse.ok(AdminAdvertiserNoteIdResponse(noteId))
    }
}
