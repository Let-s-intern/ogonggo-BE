package com.ogonggo.adminapi.advertiser.presentation

import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserService
import com.ogonggo.adminapi.advertiser.presentation.request.CreateAdminAdvertiserRequest
import com.ogonggo.adminapi.advertiser.presentation.request.UpdateAdminAdvertiserRequest
import com.ogonggo.adminapi.advertiser.presentation.response.AdminAdvertiserResponse
import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.domain.AdvertiserType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
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
@RequestMapping("/api/v1/admin/advertisers")
class AdminAdvertiserController(
    private val adminAdvertiserService: AdminAdvertiserService,
) : AdminAdvertiserApi {

    @GetMapping
    override fun getAdvertisers(
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "type", required = false) type: String?,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserResponse>>> {
        val condition = AdvertiserSearchCondition(
            type = type?.takeIf { it.isNotBlank() }?.let(::advertiserType),
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
        )
        return SuccessResponse.ok(adminAdvertiserService.getAdvertisers(condition).map(AdminAdvertiserResponse::from))
    }

    @GetMapping("/{advertiserId}")
    override fun getAdvertiser(
        @PathVariable("advertiserId") advertiserId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>> =
        SuccessResponse.ok(AdminAdvertiserResponse.from(adminAdvertiserService.getAdvertiser(advertiserId)))

    /** 등록이 커밋된 뒤의 광고주를 다시 읽어 응답한다. */
    @PostMapping
    override fun createAdvertiser(
        @RequestBody request: CreateAdminAdvertiserRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>> {
        val advertiserId = adminAdvertiserService.createAdvertiser(request.toCommand())
        return SuccessResponse.created(AdminAdvertiserResponse.from(adminAdvertiserService.getAdvertiser(advertiserId)))
    }

    /** 수정이 커밋된 뒤의 광고주를 다시 읽어 응답한다. */
    @PatchMapping("/{advertiserId}")
    override fun updateAdvertiser(
        @PathVariable("advertiserId") advertiserId: Long,
        @RequestBody request: UpdateAdminAdvertiserRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>> {
        adminAdvertiserService.updateAdvertiser(advertiserId, request.toCommand())
        return SuccessResponse.ok(AdminAdvertiserResponse.from(adminAdvertiserService.getAdvertiser(advertiserId)))
    }

    /** 모르는 값은 화면 문구로 알린다. enum 바인딩에 맡기면 기본 400 문구가 나간다. */
    private fun advertiserType(value: String): AdvertiserType =
        AdvertiserType.entries.firstOrNull { it.name == value }
            ?: throw AdminConsoleBadRequestException.field(
                "type",
                "${AdvertiserType.entries.joinToString(", ") { it.name }} 중 하나를 보내 주세요.",
            )
}
