package com.ogonggo.userapi.adposting.presentation

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.userapi.adposting.business.UserAdPostingService
import com.ogonggo.userapi.adposting.presentation.response.UserAdPostingResponse
import com.ogonggo.userapi.error.InvalidRequestParameterException
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/ad-postings")
class UserAdPostingController(
    private val userAdPostingService: UserAdPostingService,
) : UserAdPostingApi {

    /** 없는 값과 모르는 값을 같은 문구로 알리려고 문자열로 받는다. */
    @GetMapping
    override fun getAdPostings(
        @RequestParam(name = "placement", required = false) placement: String?,
    ): ResponseEntity<SuccessResponse<List<UserAdPostingResponse>>> {
        val value = AdPostingPlacement.entries.firstOrNull { it.name == placement }
            ?: throw InvalidRequestParameterException(
                "placement",
                "${AdPostingPlacement.entries.joinToString(", ") { it.name }} 중 하나를 보내 주세요.",
            )
        return SuccessResponse.ok(userAdPostingService.getAdPostings(value).map(UserAdPostingResponse::from))
    }
}
