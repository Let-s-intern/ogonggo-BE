package com.ogonggo.adminapi.advertisementexposure.presentation

import com.ogonggo.adminapi.advertisementexposure.business.AdminAdvertisementExposureService
import com.ogonggo.adminapi.advertisementexposure.presentation.request.ChangeAdminAdvertisementExposureExposedRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.CreateAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.MoveAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.UpdateAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.response.AdminAdvertisementExposureResponse
import com.ogonggo.adminapi.advertisementexposure.presentation.response.CreateAdminAdvertisementExposureResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-exposures")
class AdminAdvertisementExposureController(
    private val adminAdvertisementExposureService: AdminAdvertisementExposureService,
) : AdminAdvertisementExposureApi {

    @GetMapping
    override fun getExposures(
        @RequestParam(name = "productId") productId: Long,
        @RequestParam(name = "contractId", required = false) contractId: Long?,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertisementExposureResponse>>> =
        SuccessResponse.ok(
            adminAdvertisementExposureService.getExposures(productId, contractId)
                .map(AdminAdvertisementExposureResponse::from),
        )

    @PostMapping
    override fun createExposure(
        @RequestBody request: CreateAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementExposureResponse>> =
        SuccessResponse.created(
            CreateAdminAdvertisementExposureResponse.from(
                adminAdvertisementExposureService.createExposure(request.toCommand()),
            ),
        )

    @PutMapping("/{exposureId}")
    override fun updateExposure(
        @PathVariable(name = "exposureId") exposureId: Long,
        @RequestBody request: UpdateAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementExposureService.updateExposure(request.toCommand(exposureId))
        return SuccessResponse.ok()
    }

    @PatchMapping("/{exposureId}/exposed")
    override fun changeExposureExposed(
        @PathVariable(name = "exposureId") exposureId: Long,
        @RequestBody request: ChangeAdminAdvertisementExposureExposedRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementExposureService.changeExposed(exposureId, request.exposed)
        return SuccessResponse.ok()
    }

    @PostMapping("/{exposureId}/move")
    override fun moveExposure(
        @PathVariable(name = "exposureId") exposureId: Long,
        @RequestBody request: MoveAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementExposureService.moveExposure(exposureId, request.direction)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{exposureId}")
    override fun deleteExposure(
        @PathVariable(name = "exposureId") exposureId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementExposureService.deleteExposure(exposureId)
        return SuccessResponse.ok()
    }
}
