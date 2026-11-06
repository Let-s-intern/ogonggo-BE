package com.ogonggo.adminapi.adchannel.presentation

import com.ogonggo.adminapi.adchannel.business.AdminAdChannelService
import com.ogonggo.adminapi.adchannel.presentation.request.CreateAdminAdChannelRequest
import com.ogonggo.adminapi.adchannel.presentation.request.UpdateAdminAdChannelRequest
import com.ogonggo.adminapi.adchannel.presentation.response.AdminAdChannelIdResponse
import com.ogonggo.adminapi.adchannel.presentation.response.AdminAdChannelResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/ad-channels")
class AdminAdChannelController(
    private val adminAdChannelService: AdminAdChannelService,
) : AdminAdChannelApi {

    @GetMapping
    override fun getChannels(): ResponseEntity<SuccessResponse<List<AdminAdChannelResponse>>> =
        SuccessResponse.ok(adminAdChannelService.getChannels().map(AdminAdChannelResponse::from))

    /** 등록이 커밋된 뒤의 채널을 다시 읽어 응답한다. */
    @PostMapping
    override fun createChannel(
        @RequestBody request: CreateAdminAdChannelRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelResponse>> {
        val channelId = adminAdChannelService.createChannel(request.toCommand())
        return SuccessResponse.created(AdminAdChannelResponse.from(adminAdChannelService.getChannel(channelId)))
    }

    /** 수정이 커밋된 뒤의 채널을 다시 읽어 응답한다. */
    @PatchMapping("/{channelId}")
    override fun updateChannel(
        @PathVariable("channelId") channelId: Long,
        @RequestBody request: UpdateAdminAdChannelRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelResponse>> {
        adminAdChannelService.updateChannel(channelId, request.toCommand())
        return SuccessResponse.ok(AdminAdChannelResponse.from(adminAdChannelService.getChannel(channelId)))
    }

    @DeleteMapping("/{channelId}")
    override fun deleteChannel(
        @PathVariable("channelId") channelId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdChannelIdResponse>> {
        adminAdChannelService.deleteChannel(channelId)
        return SuccessResponse.ok(AdminAdChannelIdResponse(channelId))
    }
}
