package com.ogonggo.adminapi.ec2instance.implement

import com.ogonggo.adminapi.ec2instance.error.Ec2InstanceErrorCode
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InternalServerException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.Ec2Exception
import software.amazon.awssdk.services.ec2.model.InstanceStateName

/**
 * AWS EC2 인스턴스 상태를 읽고 켜고 끈다.
 *
 * 켜고 끄기는 AWS에 요청만 보내고 바로 돌아온다. 실제로 켜지거나 꺼지기까지는 수십 초가 걸리므로
 * 돌려주는 상태는 PENDING·STOPPING 같은 중간 상태이고, 콘솔은 목록을 다시 불러 최종 상태를 본다.
 * 이미 켜진 인스턴스를 켜거나 꺼진 인스턴스를 끄면 AWS가 그대로 성공시키므로 반복 요청해도 된다.
 */
@Component
class Ec2InstanceClient(
    private val ec2Client: Ec2Client,
    private val properties: Ec2InstanceProperties,
) {

    /** 인스턴스별 상태다. ID가 설정되지 않았거나 AWS가 돌려주지 않은 인스턴스는 빠진다. */
    fun readStates(instances: List<Ec2Instance>): Map<Ec2Instance, Ec2InstanceState> {
        val byInstanceId = instances
            .mapNotNull { instance -> properties.instanceId(instance)?.let { it to instance } }
            .toMap()
        if (byInstanceId.isEmpty()) return emptyMap()
        val response = call("describe", byInstanceId.keys.joinToString()) {
            ec2Client.describeInstances { it.instanceIds(byInstanceId.keys) }
        }
        return response.reservations()
            .flatMap { it.instances() }
            .mapNotNull { found -> byInstanceId[found.instanceId()]?.let { it to found.state().name().toState() } }
            .toMap()
    }

    fun start(instance: Ec2Instance): Ec2InstanceState {
        val instanceId = configuredInstanceId(instance)
        val response = call("start", instanceId) {
            ec2Client.startInstances { it.instanceIds(instanceId) }
        }
        return response.startingInstances().single().currentState().name().toState()
    }

    fun stop(instance: Ec2Instance): Ec2InstanceState {
        val instanceId = configuredInstanceId(instance)
        val response = call("stop", instanceId) {
            ec2Client.stopInstances { it.instanceIds(instanceId) }
        }
        return response.stoppingInstances().single().currentState().name().toState()
    }

    private fun configuredInstanceId(instance: Ec2Instance): String =
        properties.instanceId(instance) ?: run {
            log.error("EC2 인스턴스 ID가 설정되지 않았습니다. instance={}", instance)
            throw InternalServerException(Ec2InstanceErrorCode.EC2_INSTANCE_NOT_CONFIGURED)
        }

    private fun <T> call(action: String, instanceIds: String, request: () -> T): T =
        try {
            request()
        } catch (exception: Ec2Exception) {
            val errorCode = exception.awsErrorDetails()?.errorCode()
            if (errorCode == INCORRECT_INSTANCE_STATE) {
                log.warn("EC2 인스턴스가 지금 상태에서 요청을 받지 않습니다. action={}, instanceIds={}", action, instanceIds)
                throw ConflictException(Ec2InstanceErrorCode.EC2_INSTANCE_STATE_CONFLICT)
            }
            log.error(
                "EC2 호출을 AWS가 거절했습니다. action={}, instanceIds={}, errorCode={}",
                action,
                instanceIds,
                errorCode,
                exception,
            )
            throw InternalServerException(Ec2InstanceErrorCode.EC2_UNAVAILABLE)
        } catch (exception: SdkException) {
            log.error("EC2 호출에 실패했습니다. action={}, instanceIds={}", action, instanceIds, exception)
            throw InternalServerException(Ec2InstanceErrorCode.EC2_UNAVAILABLE)
        }

    private fun InstanceStateName?.toState(): Ec2InstanceState = when (this) {
        InstanceStateName.PENDING -> Ec2InstanceState.PENDING
        InstanceStateName.RUNNING -> Ec2InstanceState.RUNNING
        InstanceStateName.STOPPING -> Ec2InstanceState.STOPPING
        InstanceStateName.STOPPED -> Ec2InstanceState.STOPPED
        InstanceStateName.SHUTTING_DOWN -> Ec2InstanceState.SHUTTING_DOWN
        InstanceStateName.TERMINATED -> Ec2InstanceState.TERMINATED
        InstanceStateName.UNKNOWN_TO_SDK_VERSION, null -> Ec2InstanceState.UNKNOWN
    }

    companion object {
        private val log = LoggerFactory.getLogger(Ec2InstanceClient::class.java)

        /** 끄는 중인 인스턴스를 켜는 것처럼 지금 상태에서 받을 수 없는 요청에 AWS가 주는 오류 코드다. */
        private const val INCORRECT_INSTANCE_STATE = "IncorrectInstanceState"
    }
}
