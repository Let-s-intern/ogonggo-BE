package com.ogonggo.adminapi.ec2instance.presentation

import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.error.InvalidRequestParameterException

/** 경로의 인스턴스 이름을 대소문자 구분 없이 읽는다. 없는 이름은 다른 경로 값 오류와 같은 400이다. */
internal fun parseEc2Instance(value: String): Ec2Instance =
    Ec2Instance.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw InvalidRequestParameterException(EC2_INSTANCE_PATH_VARIABLE, "REDASH 또는 CRAWLER만 쓸 수 있습니다.")

internal const val EC2_INSTANCE_PATH_VARIABLE = "instance"
