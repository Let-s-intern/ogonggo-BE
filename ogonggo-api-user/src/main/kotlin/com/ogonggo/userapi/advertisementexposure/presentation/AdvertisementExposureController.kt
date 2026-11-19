package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureService
import com.ogonggo.userapi.advertisementexposure.presentation.response.AdvertisementExposureResponse
import com.ogonggo.userapi.error.InvalidRequestParameterException
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.response.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/advertisement-exposures")
class AdvertisementExposureController(
    private val advertisementExposureService: AdvertisementExposureService,
) : AdvertisementExposureApi {

    @GetMapping
    override fun getShown(
        @RequestParam("position") position: AdvertisementProductPosition,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdvertisementExposureResponse>>> {
        // 광고 공고 자리는 이미지·링크가 없어 배너 응답으로 줄 수 없다. 광고 공고는 /postings가 준다.
        if (position.category != AdvertisementProductCategory.BANNER) {
            throw InvalidRequestParameterException("position", "배너 자리만 조회할 수 있습니다.")
        }
        val result = advertisementExposureService.getShown(position, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdvertisementExposureResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
