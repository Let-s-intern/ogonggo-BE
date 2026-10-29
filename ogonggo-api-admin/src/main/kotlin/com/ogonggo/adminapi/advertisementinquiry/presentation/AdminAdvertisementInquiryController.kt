package com.ogonggo.adminapi.advertisementinquiry.presentation

import com.ogonggo.adminapi.advertisementinquiry.business.AdminAdvertisementInquiryService
import com.ogonggo.adminapi.advertisementinquiry.presentation.request.ChangeAdvertisementInquiryMemoRequest
import com.ogonggo.adminapi.advertisementinquiry.presentation.request.ChangeAdvertisementInquiryStatusRequest
import com.ogonggo.adminapi.advertisementinquiry.presentation.response.AdminAdvertisementInquiryResponse
import com.ogonggo.adminapi.advertisementinquiry.presentation.response.AdminAdvertisementInquiryTodayCountResponse
import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-inquiries")
class AdminAdvertisementInquiryController(
    private val adminAdvertisementInquiryService: AdminAdvertisementInquiryService,
) : AdminAdvertisementInquiryApi {

    @GetMapping
    override fun getAdvertisementInquiries(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "inquiryType", required = false) inquiryType: AdvertisementInquiryType?,
        @RequestParam(name = "campaignCode", required = false) campaignCode: String?,
        @RequestParam(name = "status", required = false) statuses: List<AdvertisementInquiryStatus>?,
        @RequestParam(name = "memberType", required = false) memberType: AdvertisementInquiryMemberType?,
        @RequestParam(name = "inflowChannel", required = false) inflowChannel: AdvertisementInquiryInflowChannel?,
        @RequestParam(name = "companyNameSource", required = false) companyNameSource: AdvertisementInquiryCompanyNameSource?,
        @RequestParam(name = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        createdFrom: LocalDate?,
        @RequestParam(name = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        createdTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementInquiryResponse>>> {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw InvalidRequestParameterException("createdFrom", "접수 기간 시작일은 종료일보다 늦을 수 없습니다.")
        }
        val result = adminAdvertisementInquiryService.getAdvertisementInquiries(
            condition = AdvertisementInquirySearchCondition(
                inquiryType = inquiryType,
                campaignCode = campaignCode?.takeIf { it.isNotBlank() },
                statuses = statuses.orEmpty().toSet(),
                memberType = memberType,
                inflowChannel = inflowChannel,
                companyNameSource = companyNameSource,
                createdFrom = createdFrom,
                createdTo = createdTo,
            ),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminAdvertisementInquiryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/today-count")
    override fun getTodayCount(): ResponseEntity<SuccessResponse<AdminAdvertisementInquiryTodayCountResponse>> =
        SuccessResponse.ok(AdminAdvertisementInquiryTodayCountResponse(adminAdvertisementInquiryService.getTodayCount()))

    @PatchMapping("/{inquiryId}/status")
    override fun changeStatus(
        @PathVariable("inquiryId") inquiryId: Long,
        @RequestBody request: ChangeAdvertisementInquiryStatusRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementInquiryResponse>> =
        SuccessResponse.ok(
            AdminAdvertisementInquiryResponse.from(adminAdvertisementInquiryService.changeStatus(inquiryId, request.status)),
        )

    @PatchMapping("/{inquiryId}/memo")
    override fun changeMemo(
        @PathVariable("inquiryId") inquiryId: Long,
        @RequestBody request: ChangeAdvertisementInquiryMemoRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementInquiryResponse>> =
        SuccessResponse.ok(
            AdminAdvertisementInquiryResponse.from(adminAdvertisementInquiryService.changeMemo(inquiryId, request.memo)),
        )
}
