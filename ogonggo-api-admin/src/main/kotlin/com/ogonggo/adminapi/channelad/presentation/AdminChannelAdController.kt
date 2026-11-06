package com.ogonggo.adminapi.channelad.presentation

import com.ogonggo.adminapi.channelad.business.AdminChannelAdSearchCommand
import com.ogonggo.adminapi.channelad.business.AdminChannelAdService
import com.ogonggo.adminapi.channelad.presentation.request.CreateAdminChannelAdRequest
import com.ogonggo.adminapi.channelad.presentation.request.UpdateAdminChannelAdRequest
import com.ogonggo.adminapi.channelad.presentation.response.AdminChannelAdIdResponse
import com.ogonggo.adminapi.channelad.presentation.response.AdminChannelAdResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/channel-ads")
class AdminChannelAdController(
    private val adminChannelAdService: AdminChannelAdService,
) : AdminChannelAdApi {

    @GetMapping
    override fun getChannelAds(
        @RequestParam("month", required = false) month: String?,
        @RequestParam("advertiserId", required = false) advertiserId: Long?,
        @RequestParam("contractId", required = false) contractId: Long?,
        @RequestParam("platform", required = false) platform: AdChannelPlatform?,
        @RequestParam("channelId", required = false) channelId: Long?,
        @RequestParam("formatId", required = false) formatId: Long?,
        @RequestParam("status", required = false) status: ChannelAdStatus?,
    ): ResponseEntity<SuccessResponse<List<AdminChannelAdResponse>>> {
        val command = AdminChannelAdSearchCommand(
            month = month,
            advertiserId = advertiserId,
            contractId = contractId,
            platform = platform,
            channelId = channelId,
            formatId = formatId,
            status = status,
        )
        return SuccessResponse.ok(adminChannelAdService.getChannelAds(command).map(AdminChannelAdResponse::from))
    }

    /** 등록이 커밋된 뒤의 광고를 다시 읽어 응답한다. */
    @PostMapping
    override fun createChannelAd(
        @RequestBody request: CreateAdminChannelAdRequest,
    ): ResponseEntity<SuccessResponse<AdminChannelAdResponse>> {
        val adId = adminChannelAdService.createChannelAd(request.toCommand())
        return SuccessResponse.created(AdminChannelAdResponse.from(adminChannelAdService.getChannelAd(adId)))
    }

    /** 수정이 커밋된 뒤의 광고를 다시 읽어 응답한다. */
    @PatchMapping("/{adId}")
    override fun updateChannelAd(
        @PathVariable("adId") adId: Long,
        @RequestBody request: UpdateAdminChannelAdRequest,
    ): ResponseEntity<SuccessResponse<AdminChannelAdResponse>> {
        adminChannelAdService.updateChannelAd(adId, request.toCommand())
        return SuccessResponse.ok(AdminChannelAdResponse.from(adminChannelAdService.getChannelAd(adId)))
    }

    @DeleteMapping("/{adId}")
    override fun deleteChannelAd(
        @PathVariable("adId") adId: Long,
    ): ResponseEntity<SuccessResponse<AdminChannelAdIdResponse>> {
        adminChannelAdService.deleteChannelAd(adId)
        return SuccessResponse.ok(AdminChannelAdIdResponse(adId))
    }
}
