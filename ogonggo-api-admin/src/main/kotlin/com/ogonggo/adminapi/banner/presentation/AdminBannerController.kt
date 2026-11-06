package com.ogonggo.adminapi.banner.presentation

import com.ogonggo.adminapi.banner.business.AdminBannerService
import com.ogonggo.adminapi.banner.presentation.request.CreateAdminBannerRequest
import com.ogonggo.adminapi.banner.presentation.request.ReplaceAdminBannerOrderRequest
import com.ogonggo.adminapi.banner.presentation.request.UpdateAdminBannerRequest
import com.ogonggo.adminapi.banner.presentation.response.AdminBannerIdResponse
import com.ogonggo.adminapi.banner.presentation.response.AdminBannerResponse
import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.banner.domain.BannerPlacement
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
@RequestMapping("/api/v1/admin/banners")
class AdminBannerController(
    private val adminBannerService: AdminBannerService,
) : AdminBannerApi {

    /** 쿼리 값은 문자열로 받아 화면 문구로 알린다. enum·숫자 바인딩에 맡기면 기본 400 문구가 나간다. */
    @GetMapping
    override fun getBanners(
        @RequestParam(name = "placement", required = false) placement: String?,
        @RequestParam(name = "advertiserId", required = false) advertiserId: String?,
    ): ResponseEntity<SuccessResponse<List<AdminBannerResponse>>> {
        val advertiser = advertiserId?.takeIf { it.isNotBlank() }?.let {
            it.trim().toLongOrNull() ?: throw AdminConsoleBadRequestException.field("advertiserId", "광고주 id 를 숫자로 보내 주세요.")
        }
        val bannerPlacement = placement?.takeIf { it.isNotBlank() }?.let { value ->
            BannerPlacement.entries.firstOrNull { it.name == value } ?: throw AdminBannerService.placementError()
        }
        if (bannerPlacement == null && advertiser == null) {
            throw AdminBannerService.placementError()
        }
        return SuccessResponse.ok(adminBannerService.getBanners(bannerPlacement, advertiser).map(AdminBannerResponse::from))
    }

    /** 등록이 커밋된 뒤의 배너를 다시 읽어 응답한다. */
    @PostMapping
    override fun createBanner(
        @RequestBody request: CreateAdminBannerRequest,
    ): ResponseEntity<SuccessResponse<AdminBannerResponse>> {
        val bannerId = adminBannerService.createBanner(request.toCommand())
        return SuccessResponse.created(AdminBannerResponse.from(adminBannerService.getBanner(bannerId)))
    }

    /** 바뀐 순서가 커밋된 뒤 그 자리를 다시 읽어 응답한다. */
    @PutMapping("/order")
    override fun replaceBannerOrder(
        @RequestBody request: ReplaceAdminBannerOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminBannerResponse>>> {
        adminBannerService.replaceOrder(request.placement, request.bannerIds)
        val placement = checkNotNull(request.placement)
        return SuccessResponse.ok(adminBannerService.getBanners(placement, null).map(AdminBannerResponse::from))
    }

    /** 수정이 커밋된 뒤의 배너를 다시 읽어 응답한다. */
    @PatchMapping("/{bannerId}")
    override fun updateBanner(
        @PathVariable("bannerId") bannerId: Long,
        @RequestBody request: UpdateAdminBannerRequest,
    ): ResponseEntity<SuccessResponse<AdminBannerResponse>> {
        adminBannerService.updateBanner(bannerId, request.toCommand())
        return SuccessResponse.ok(AdminBannerResponse.from(adminBannerService.getBanner(bannerId)))
    }

    @DeleteMapping("/{bannerId}")
    override fun deleteBanner(
        @PathVariable("bannerId") bannerId: Long,
    ): ResponseEntity<SuccessResponse<AdminBannerIdResponse>> {
        adminBannerService.deleteBanner(bannerId)
        return SuccessResponse.ok(AdminBannerIdResponse(bannerId))
    }
}
