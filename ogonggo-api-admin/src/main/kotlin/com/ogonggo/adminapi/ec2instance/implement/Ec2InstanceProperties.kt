package com.ogonggo.adminapi.ec2instance.implement

import com.ogonggo.adminapi.ec2instance.error.Ec2InstanceErrorCode
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceTarget
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 관리자 콘솔에서 켜고 끌 수 있는 EC2 인스턴스 목록이다. 키는 콘솔이 부르는 이름, 값은 인스턴스 ID다.
 *
 * 인스턴스 ID는 비밀이 아니고, 운영 설정(application.yml)은 GitHub 시크릿이라 고치기 번거로워 기본값을 코드에 둔다.
 * 인스턴스를 바꾸거나 늘리면 설정에서 목록 전체를 다시 적는다(맵은 기본값과 합쳐지지 않고 통째로 바뀐다).
 *
 * ```yaml
 * ogonggo:
 *   ec2-instance:
 *     instances:
 *       redash: i-0b277b7cfbfe0d9c2
 *       crawler: i-0eaf16dee5d8e17f3
 * ```
 *
 * AWS 인증은 S3와 같은 `cloud.aws` 설정을 쓴다. 그 IAM 사용자에게 ec2:DescribeInstances, ec2:StartInstances,
 * ec2:StopInstances 권한이 있어야 한다.
 */
@ConfigurationProperties(prefix = "ogonggo.ec2-instance")
data class Ec2InstanceProperties(
    val instances: Map<String, String> = DEFAULT_INSTANCES,
) {
    /** 설정 순서대로 돌려준다. 콘솔 목록 순서가 매번 같도록 한다. */
    fun targets(): List<Ec2InstanceTarget> = instances.map { (name, instanceId) -> Ec2InstanceTarget(name, instanceId) }

    fun target(name: String): Ec2InstanceTarget {
        val instanceId = instances[name]
            ?: throw EntityNotFoundException(
                Ec2InstanceErrorCode.EC2_INSTANCE_NOT_FOUND,
                "${Ec2InstanceErrorCode.EC2_INSTANCE_NOT_FOUND.message} (name: $name)",
            )
        return Ec2InstanceTarget(name, instanceId)
    }

    companion object {
        val DEFAULT_INSTANCES: Map<String, String> = linkedMapOf(
            "redash" to "i-0b277b7cfbfe0d9c2",
            "crawler" to "i-0eaf16dee5d8e17f3",
        )
    }
}
