package com.ogonggo.adminapi.ec2instance.business

import com.ogonggo.adminapi.ec2instance.implement.Ec2InstanceClient
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import org.springframework.stereotype.Service

/**
 * 운영자가 리대시·크롤러 EC2 인스턴스를 켜고 끄는 유스케이스다. DB에 남기지 않는다.
 *
 * 크롤러는 EventBridge Scheduler, 리대시는 SSM Quick Setup이 매일 정해진 시각에 켜고 끈다.
 * 여기서 켜도 그 시각이 되면 다시 꺼진다.
 */
@Service
class AdminEc2InstanceService(
    private val ec2InstanceClient: Ec2InstanceClient,
) {

    fun list(): List<AdminEc2InstanceResult> {
        val instances = Ec2Instance.entries
        val states = ec2InstanceClient.readStates(instances)
        return instances.map { AdminEc2InstanceResult(it, states[it] ?: Ec2InstanceState.UNKNOWN) }
    }

    fun start(instance: Ec2Instance): AdminEc2InstanceResult =
        AdminEc2InstanceResult(instance, ec2InstanceClient.start(instance))

    fun stop(instance: Ec2Instance): AdminEc2InstanceResult =
        AdminEc2InstanceResult(instance, ec2InstanceClient.stop(instance))
}

data class AdminEc2InstanceResult(
    val instance: Ec2Instance,
    val state: Ec2InstanceState,
)
