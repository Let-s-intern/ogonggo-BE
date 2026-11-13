package com.ogonggo.adminapi.ec2instance.implement.dto

/** 콘솔이 부르는 이름과 AWS 인스턴스 ID의 짝이다. */
data class Ec2InstanceTarget(
    val name: String,
    val instanceId: String,
)

/**
 * AWS EC2 인스턴스 상태다. AWS의 InstanceStateName을 그대로 옮긴다.
 * 업무 enum이 아니라 외부 시스템 값이므로 EnumField를 구현하지 않는다.
 */
enum class Ec2InstanceState {
    PENDING,
    RUNNING,
    STOPPING,
    STOPPED,
    SHUTTING_DOWN,
    TERMINATED,

    /** SDK가 아직 모르는 새 상태 값이다. */
    UNKNOWN,
}
