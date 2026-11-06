package com.ogonggo.userapi.banner.presentation

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.banner.business.UserBannerService
import com.ogonggo.userapi.banner.presentation.response.UserBannerResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/banners")
class UserBannerController(
    private val userBannerService: UserBannerService,
) : UserBannerApi {

    @GetMapping
    override fun getBanners(
        @RequestParam(name = "placement") placement: BannerPlacement,
    ): ResponseEntity<SuccessResponse<List<UserBannerResponse>>> =
        SuccessResponse.ok(userBannerService.getBanners(placement).map(UserBannerResponse::from))
}
