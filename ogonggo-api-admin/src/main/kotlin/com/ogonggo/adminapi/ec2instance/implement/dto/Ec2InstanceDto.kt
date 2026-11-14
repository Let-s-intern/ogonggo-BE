package com.ogonggo.adminapi.ec2instance.implement.dto

import com.ogonggo.core.enumeration.EnumField

/**
 * 관리자 콘솔에서 켜고 끌 수 있는 EC2 인스턴스다. 콘솔은 이 이름으로만 고르고 인스턴스 ID는 모른다.
 * 실제 인스턴스 ID는 코드에 두지 않고 [com.ogonggo.adminapi.ec2instance.implement.Ec2InstanceProperties] 설정에서 읽는다.
 * 인스턴스 종류를 늘릴 때만 여기를 고친다.
 */
enum class Ec2Instance(
    override val code: Int,
    override val desc: String,
) : EnumField {
    REDASH(1, "리대시"),
    CRAWLER(2, "크롤러"),
}

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
