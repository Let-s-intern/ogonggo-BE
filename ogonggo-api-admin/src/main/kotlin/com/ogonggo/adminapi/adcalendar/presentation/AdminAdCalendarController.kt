package com.ogonggo.adminapi.adcalendar.presentation

import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarService
import com.ogonggo.adminapi.adcalendar.presentation.response.AdminAdCalendarResponse
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/ad-calendar")
class AdminAdCalendarController(
    private val adminAdCalendarService: AdminAdCalendarService,
) : AdminAdCalendarApi {

    /** 빠지거나 틀린 날짜를 화면 문구로 알리려고 문자열로 받는다. */
    @GetMapping
    override fun getAdCalendar(
        @RequestParam(name = "from", required = false) from: String?,
        @RequestParam(name = "to", required = false) to: String?,
    ): ResponseEntity<SuccessResponse<AdminAdCalendarResponse>> =
        SuccessResponse.ok(AdminAdCalendarResponse.from(adminAdCalendarService.getAdCalendar(AdminCalendarDateRange.parse(from, to))))
}
