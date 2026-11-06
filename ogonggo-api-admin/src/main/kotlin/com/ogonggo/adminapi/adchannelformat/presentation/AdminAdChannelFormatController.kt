package com.ogonggo.adminapi.adchannelformat.presentation

import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatService
import com.ogonggo.adminapi.adchannelformat.presentation.request.CreateAdminAdChannelFormatRequest
import com.ogonggo.adminapi.adchannelformat.presentation.request.ReplaceAdminAdChannelFormatOrderRequest
import com.ogonggo.adminapi.adchannelformat.presentation.request.UpdateAdminAdChannelFormatRequest
import com.ogonggo.adminapi.adchannelformat.presentation.response.AdminAdChannelFormatIdResponse
import com.ogonggo.adminapi.adchannelformat.presentation.response.AdminAdChannelFormatResponse
import com.ogonggo.adminapi.response.SuccessResponse
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
import org.springframework.web.bind.annotation.RestController

/** 순서 바꾸기(`PUT /order`)는 메서드가 달라 `/{formatId}`(PATCH·DELETE)와 겹치지 않는다. 식별자 경로는 숫자만 받는다. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/ad-channel-formats")
class AdminAdChannelFormatController(
    private val adminAdChannelFormatService: AdminAdChannelFormatService,
) : AdminAdChannelFormatApi {

    @GetMapping
    override fun getFormats(): ResponseEntity<SuccessResponse<List<AdminAdChannelFormatResponse>>> =
        SuccessResponse.ok(adminAdChannelFormatService.getFormats().map(AdminAdChannelFormatResponse::from))

    /** 등록이 커밋된 뒤의 유형을 다시 읽어 응답한다. */
    @PostMapping
    override fun createFormat(
        @RequestBody request: CreateAdminAdChannelFormatRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatResponse>> {
        val formatId = adminAdChannelFormatService.createFormat(request.toCommand())
        return SuccessResponse.created(AdminAdChannelFormatResponse.from(adminAdChannelFormatService.getFormat(formatId)))
    }

    @PutMapping("/order")
    override fun replaceOrder(
        @RequestBody request: ReplaceAdminAdChannelFormatOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminAdChannelFormatResponse>>> {
        adminAdChannelFormatService.reorderFormats(request.toCommand())
        return getFormats()
    }

    /** 수정이 커밋된 뒤의 유형을 다시 읽어 응답한다. */
    @PatchMapping("/{formatId:\\d+}")
    override fun updateFormat(
        @PathVariable("formatId") formatId: Long,
        @RequestBody request: UpdateAdminAdChannelFormatRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatResponse>> {
        adminAdChannelFormatService.updateFormat(formatId, request.toCommand())
        return SuccessResponse.ok(AdminAdChannelFormatResponse.from(adminAdChannelFormatService.getFormat(formatId)))
    }

    @DeleteMapping("/{formatId:\\d+}")
    override fun deleteFormat(
        @PathVariable("formatId") formatId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatIdResponse>> {
        adminAdChannelFormatService.deleteFormat(formatId)
        return SuccessResponse.ok(AdminAdChannelFormatIdResponse(formatId))
    }
}
