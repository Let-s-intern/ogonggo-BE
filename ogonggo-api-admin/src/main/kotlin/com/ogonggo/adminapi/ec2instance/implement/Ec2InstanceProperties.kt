package com.ogonggo.adminapi.ec2instance.implement

import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * [Ec2Instance]마다 실제 AWS 인스턴스 ID다. 인스턴스를 새로 만들어 ID가 바뀌어도 코드를 고치지 않고 설정만 바꾼다.
 *
 * ```yaml
 * ogonggo:
 *   ec2-instance:
 *     redash-instance-id: i-...
 *     crawler-instance-id: i-...
 * ```
 *
 * 기본값을 빈 문자열로 둬서 값이 없어도 애플리케이션은 기동한다. 그 인스턴스를 켜고 끌 때만 실패한다.
 * AWS 인증은 S3와 같은 `cloud.aws` 설정을 쓴다. 그 IAM 사용자에게 ec2:DescribeInstances, ec2:StartInstances,
 * ec2:StopInstances 권한이 있어야 한다.
 */
@ConfigurationProperties(prefix = "ogonggo.ec2-instance")
data class Ec2InstanceProperties(
    val redashInstanceId: String = "",
    val crawlerInstanceId: String = "",
) {
    /** 설정되지 않았으면 null이다. */
    fun instanceId(instance: Ec2Instance): String? = when (instance) {
        Ec2Instance.REDASH -> redashInstanceId
        Ec2Instance.CRAWLER -> crawlerInstanceId
    }.ifBlank { null }
}
