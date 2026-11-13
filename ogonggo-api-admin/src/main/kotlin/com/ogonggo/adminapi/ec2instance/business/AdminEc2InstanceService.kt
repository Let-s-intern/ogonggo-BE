package com.ogonggo.adminapi.ec2instance.business

import com.ogonggo.adminapi.ec2instance.implement.Ec2InstanceClient
import com.ogonggo.adminapi.ec2instance.implement.Ec2InstanceProperties
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
    private val properties: Ec2InstanceProperties,
    private val ec2InstanceClient: Ec2InstanceClient,
) {

    fun list(): List<AdminEc2InstanceResult> {
        val targets = properties.targets()
        val states = ec2InstanceClient.readStates(targets)
        return targets.map {
            AdminEc2InstanceResult(it.name, it.instanceId, states[it.instanceId] ?: Ec2InstanceState.UNKNOWN)
        }
    }

    fun start(name: String): AdminEc2InstanceResult {
        val target = properties.target(name)
        return AdminEc2InstanceResult(target.name, target.instanceId, ec2InstanceClient.start(target))
    }

    fun stop(name: String): AdminEc2InstanceResult {
        val target = properties.target(name)
        return AdminEc2InstanceResult(target.name, target.instanceId, ec2InstanceClient.stop(target))
    }
}

data class AdminEc2InstanceResult(
    val name: String,
    val instanceId: String,
    val state: Ec2InstanceState,
)
