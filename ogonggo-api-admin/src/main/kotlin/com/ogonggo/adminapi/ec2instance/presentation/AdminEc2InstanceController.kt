package com.ogonggo.adminapi.ec2instance.presentation

import com.ogonggo.adminapi.ec2instance.business.AdminEc2InstanceService
import com.ogonggo.adminapi.ec2instance.presentation.response.AdminEc2InstanceResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/ec2-instances")
class AdminEc2InstanceController(
    private val adminEc2InstanceService: AdminEc2InstanceService,
) : AdminEc2InstanceApi {

    @GetMapping
    override fun listEc2Instances(): ResponseEntity<SuccessResponse<List<AdminEc2InstanceResponse>>> =
        SuccessResponse.ok(adminEc2InstanceService.list().map(AdminEc2InstanceResponse::from))

    @PostMapping("/{$EC2_INSTANCE_PATH_VARIABLE}/start")
    override fun startEc2Instance(
        @PathVariable(EC2_INSTANCE_PATH_VARIABLE) instance: String,
    ): ResponseEntity<SuccessResponse<AdminEc2InstanceResponse>> =
        SuccessResponse.ok(AdminEc2InstanceResponse.from(adminEc2InstanceService.start(parseEc2Instance(instance))))

    @PostMapping("/{$EC2_INSTANCE_PATH_VARIABLE}/stop")
    override fun stopEc2Instance(
        @PathVariable(EC2_INSTANCE_PATH_VARIABLE) instance: String,
    ): ResponseEntity<SuccessResponse<AdminEc2InstanceResponse>> =
        SuccessResponse.ok(AdminEc2InstanceResponse.from(adminEc2InstanceService.stop(parseEc2Instance(instance))))
}
