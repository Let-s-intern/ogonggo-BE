package com.ogonggo.userapi.companyname.presentation

import com.ogonggo.userapi.companyname.business.UserCompanyNameService
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/company-names")
class UserCompanyNameController(
    private val userCompanyNameService: UserCompanyNameService,
) : UserCompanyNameApi {

    @GetMapping
    override fun getCompanyNames(
        @RequestParam(name = "keyword", required = false) keyword: String?,
    ): ResponseEntity<SuccessResponse<List<String>>> =
        SuccessResponse.ok(userCompanyNameService.searchCompanyNames(keyword))
}
