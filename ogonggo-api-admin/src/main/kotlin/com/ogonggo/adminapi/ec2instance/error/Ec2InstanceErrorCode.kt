package com.ogonggo.adminapi.ec2instance.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class Ec2InstanceErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    /** 설정의 `ogonggo.ec2-instance.instances`에 없는 이름이다. 아무 인스턴스나 켜고 끄지 못하게 이름으로만 고른다. */
    EC2_INSTANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "켜고 끌 수 있는 EC2 인스턴스가 아닙니다."),

    /** 끄는 중인 인스턴스를 켜는 것처럼 AWS가 지금 상태에서 받지 않는 요청이다. 잠시 뒤 다시 시도하면 된다. */
    EC2_INSTANCE_STATE_CONFLICT(HttpStatus.CONFLICT, "EC2 인스턴스가 상태를 바꾸는 중입니다. 잠시 후 다시 시도해주세요."),

    /** AWS 호출 실패. 권한 부족(UnauthorizedOperation)도 여기에 들어가며 사유는 서버 로그에 남는다. */
    EC2_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AWS EC2 호출에 실패했습니다. 잠시 후 다시 시도해주세요."),
}
