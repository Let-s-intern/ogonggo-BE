package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.business.AiBootcampService
import com.ogonggo.adminapi.bootcamp.presentation.request.AiBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.AiBootcampLookupResponse
import com.ogonggo.adminapi.bootcamp.presentation.response.AiBootcampRegistrationResponse
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
class AiBootcampController(
    private val aiBootcampService: AiBootcampService,
) : AiBootcampApi {

    @PostMapping
    override fun registerBootcamp(
        @RequestBody request: AiBootcampRequest,
    ): ResponseEntity<SuccessResponse<AiBootcampRegistrationResponse>> =
        SuccessResponse.created(
            AiBootcampRegistrationResponse(bootcampId = aiBootcampService.register(request.toCommand())),
        )

    @GetMapping
    override fun getBootcamp(
        @RequestParam(name = "sourceUrl") sourceUrl: String,
    ): ResponseEntity<SuccessResponse<AiBootcampLookupResponse>> =
        SuccessResponse.ok(
            AiBootcampLookupResponse(bootcampId = aiBootcampService.getBootcampId(sourceUrl)),
        )

    @PutMapping("/{bootcampId}")
    override fun replaceBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
        @RequestBody request: AiBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiBootcampService.replace(bootcampId, request.toCommand())
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{bootcampId}")
    override fun deleteBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiBootcampService.delete(bootcampId)
        return SuccessResponse.ok()
    }
}
