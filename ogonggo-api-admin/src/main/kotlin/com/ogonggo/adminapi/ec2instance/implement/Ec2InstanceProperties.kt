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
 *     access-key: ...
 *     secret-key: ...
 * ```
 *
 * 기본값을 빈 문자열로 둬서 값이 없어도 애플리케이션은 기동한다. 그 인스턴스를 켜고 끌 때만 실패한다.
 * 리대시·크롤러는 S3 버킷과 다른 AWS 계정에 있어서 `cloud.aws` 접근 키를 쓸 수 없다. 인스턴스가 있는 계정의
 * IAM 사용자 접근 키를 [accessKey]·[secretKey]로 따로 받는다. 그 사용자에게 ec2:DescribeInstances,
 * ec2:StartInstances, ec2:StopInstances 권한이 있어야 한다. 접근 키가 없으면 AWS 기본 인증(환경변수, ECS 태스크 역할 등)을 쓴다.
 */
@ConfigurationProperties(prefix = "ogonggo.ec2-instance")
data class Ec2InstanceProperties(
    val redashInstanceId: String = "",
    val crawlerInstanceId: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
) {
    val credentialsConfigured: Boolean get() = accessKey.isNotBlank() && secretKey.isNotBlank()

    /** 설정되지 않았으면 null이다. */
    fun instanceId(instance: Ec2Instance): String? = when (instance) {
        Ec2Instance.REDASH -> redashInstanceId
        Ec2Instance.CRAWLER -> crawlerInstanceId
    }.ifBlank { null }
}
