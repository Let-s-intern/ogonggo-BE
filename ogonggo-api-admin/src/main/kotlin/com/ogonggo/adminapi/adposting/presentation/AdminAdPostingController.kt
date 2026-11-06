package com.ogonggo.adminapi.adposting.presentation

import com.ogonggo.adminapi.adposting.business.AdminAdPostingService
import com.ogonggo.adminapi.adposting.presentation.request.CreateAdminAdPostingRequest
import com.ogonggo.adminapi.adposting.presentation.request.ReplaceAdminAdPostingOrderRequest
import com.ogonggo.adminapi.adposting.presentation.request.UpdateAdminAdPostingRequest
import com.ogonggo.adminapi.adposting.presentation.response.AdminAdPostingIdResponse
import com.ogonggo.adminapi.adposting.presentation.response.AdminAdPostingResponse
import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/ad-postings")
class AdminAdPostingController(
    private val adminAdPostingService: AdminAdPostingService,
) : AdminAdPostingApi {

    /** 모르는 값은 화면 문구로 알리려고 문자열로 받는다. enum·숫자 바인딩에 맡기면 기본 400 문구가 나간다. */
    @GetMapping
    override fun getAdPostings(
        @RequestParam(name = "placement", required = false) placement: String?,
        @RequestParam(name = "advertiserId", required = false) advertiserId: String?,
    ): ResponseEntity<SuccessResponse<List<AdminAdPostingResponse>>> {
        val advertiser = advertiserId?.takeIf { it.isNotBlank() }?.let(::advertiserIdOf)
        val adPostings = adminAdPostingService.getAdPostings(placement?.takeIf { it.isNotBlank() }?.let(::placementOf), advertiser)
        return SuccessResponse.ok(adPostings.map(AdminAdPostingResponse::from))
    }

    /** 등록이 커밋된 뒤의 광고 공고를 다시 읽어 응답한다. */
    @PostMapping
    override fun createAdPosting(
        @RequestBody request: CreateAdminAdPostingRequest,
    ): ResponseEntity<SuccessResponse<AdminAdPostingResponse>> {
        val adPostingId = adminAdPostingService.createAdPosting(request.toCommand())
        return SuccessResponse.created(AdminAdPostingResponse.from(adminAdPostingService.getAdPosting(adPostingId)))
    }

    /** 수정이 커밋된 뒤의 광고 공고를 다시 읽어 응답한다. */
    @PatchMapping("/{adPostingId}")
    override fun updateAdPosting(
        @PathVariable("adPostingId") adPostingId: Long,
        @RequestBody request: UpdateAdminAdPostingRequest,
    ): ResponseEntity<SuccessResponse<AdminAdPostingResponse>> {
        adminAdPostingService.updateAdPosting(adPostingId, request.toCommand())
        return SuccessResponse.ok(AdminAdPostingResponse.from(adminAdPostingService.getAdPosting(adPostingId)))
    }

    @DeleteMapping("/{adPostingId}")
    override fun deleteAdPosting(
        @PathVariable("adPostingId") adPostingId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdPostingIdResponse>> {
        adminAdPostingService.deleteAdPosting(adPostingId)
        return SuccessResponse.ok(AdminAdPostingIdResponse(adPostingId))
    }

    /** 순서 변경이 커밋된 뒤의 자리를 다시 읽어 응답한다. */
    @PutMapping("/order")
    override fun replaceAdPostingOrder(
        @RequestBody request: ReplaceAdminAdPostingOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminAdPostingResponse>>> {
        val placement = adminAdPostingService.replaceOrder(request.toCommand())
        return SuccessResponse.ok(adminAdPostingService.getAdPostings(placement, null).map(AdminAdPostingResponse::from))
    }

    private fun placementOf(value: String): AdPostingPlacement =
        AdPostingPlacement.entries.firstOrNull { it.name == value } ?: throw AdminAdPostingService.placementError()

    private fun advertiserIdOf(value: String): Long =
        value.toLongOrNull() ?: throw AdminConsoleBadRequestException.field("advertiserId", "광고주 id 를 숫자로 보내 주세요.")
}
