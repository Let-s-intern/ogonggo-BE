package com.ogonggo.userapi.concern.presentation

import com.ogonggo.userapi.concern.business.ConcernService
import com.ogonggo.userapi.concern.presentation.request.ConcernListRequest
import com.ogonggo.userapi.concern.presentation.request.SaveConcernRequest
import com.ogonggo.userapi.concern.presentation.response.ConcernDetailResponse
import com.ogonggo.userapi.concern.presentation.response.ConcernSummaryResponse
import com.ogonggo.userapi.concern.presentation.response.CreateConcernResponse
import com.ogonggo.userapi.concern.presentation.response.toPageResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/concerns")
class ConcernController(
    private val concernService: ConcernService,
) : ConcernApi {

    @GetMapping
    override fun listConcerns(
        @ModelAttribute request: ConcernListRequest,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernSummaryResponse>>> =
        SuccessResponse.ok(concernService.readConcerns(request.toQuery()).toPageResponse())

    @GetMapping("/{concernId}")
    override fun getConcern(
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("concernId") concernId: Long,
    ): ResponseEntity<SuccessResponse<ConcernDetailResponse>> =
        SuccessResponse.ok(ConcernDetailResponse.from(concernService.readConcern(userId, concernId)))

    @PostMapping
    override fun createConcern(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: SaveConcernRequest,
    ): ResponseEntity<SuccessResponse<CreateConcernResponse>> =
        SuccessResponse.created(CreateConcernResponse(concernService.create(userId, request.toCommand())))

    @PutMapping("/{concernId}")
    override fun replaceConcern(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
        @RequestBody request: SaveConcernRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        concernService.update(userId, concernId, request.toCommand())
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{concernId}")
    override fun deleteConcern(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        concernService.delete(userId, concernId)
        return SuccessResponse.ok()
    }
}
