package com.ogonggo.userapi.enumeration.presentation

import com.ogonggo.userapi.enumeration.business.UserEnumService
import com.ogonggo.userapi.enumeration.presentation.response.UserEnumOptionResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/enums")
class UserEnumController(
    private val userEnumService: UserEnumService,
) : UserEnumApi {

    @GetMapping
    override fun getEnums(): ResponseEntity<SuccessResponse<Map<String, List<UserEnumOptionResponse>>>> =
        SuccessResponse.ok(
            userEnumService.getEnums().mapValues { (_, options) -> options.map(UserEnumOptionResponse::from) },
        )
}
