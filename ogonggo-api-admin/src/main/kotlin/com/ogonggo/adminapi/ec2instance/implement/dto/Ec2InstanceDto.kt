package com.ogonggo.adminapi.ec2instance.implement.dto

import com.ogonggo.core.enumeration.EnumField

/**
 * 관리자 콘솔에서 켜고 끌 수 있는 EC2 인스턴스다. 콘솔은 이 이름으로만 고르고 인스턴스 ID는 모른다.
 *
 * [instanceId]는 JSON과 `/enums` 응답에 나가지 않는다. enum은 이름으로 직렬화되고 선택지는 이름·설명만 내보낸다.
 * 인스턴스를 바꾸거나 늘리면 여기를 고친다. 인스턴스 ID는 비밀이 아니지만 콘솔이 알 필요가 없다.
 */
enum class Ec2Instance(
    override val code: Int,
    override val desc: String,
    val instanceId: String,
) : EnumField {
    REDASH(1, "리대시", "i-0b277b7cfbfe0d9c2"),
    CRAWLER(2, "크롤러", "i-0eaf16dee5d8e17f3"),
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
