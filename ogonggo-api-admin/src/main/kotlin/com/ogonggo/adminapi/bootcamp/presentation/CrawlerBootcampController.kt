package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampService
import com.ogonggo.adminapi.bootcamp.presentation.request.CrawlerBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.CrawlerBootcampLookupResponse
import com.ogonggo.adminapi.bootcamp.presentation.response.CrawlerBootcampRegistrationResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/internal/bootcamps")
class CrawlerBootcampController(
    private val crawlerBootcampService: CrawlerBootcampService,
) : CrawlerBootcampApi {

    @PostMapping
    override fun registerBootcamp(
        @RequestBody request: CrawlerBootcampRequest,
    ): ResponseEntity<SuccessResponse<CrawlerBootcampRegistrationResponse>> =
        SuccessResponse.created(
            CrawlerBootcampRegistrationResponse(bootcampId = crawlerBootcampService.register(request.toCommand())),
        )

    @GetMapping
    override fun getBootcamp(
        @RequestParam(name = "sourceUrl") sourceUrl: String,
    ): ResponseEntity<SuccessResponse<CrawlerBootcampLookupResponse>> =
        SuccessResponse.ok(
            CrawlerBootcampLookupResponse(bootcampId = crawlerBootcampService.getBootcampId(sourceUrl)),
        )

    @PutMapping("/{bootcampId}")
    override fun replaceBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
        @RequestBody request: CrawlerBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerBootcampService.replace(bootcampId, request.toCommand())
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{bootcampId}")
    override fun deleteBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerBootcampService.delete(bootcampId)
        return SuccessResponse.ok()
    }
}
