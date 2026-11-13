package com.ogonggo.adminapi.ec2instance.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class Ec2InstanceErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    /** 운영 설정에 그 인스턴스의 ID(`ogonggo.ec2-instance.*-instance-id`)가 없다. */
    EC2_INSTANCE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "EC2 인스턴스 ID가 설정되지 않았습니다."),

    /** 끄는 중인 인스턴스를 켜는 것처럼 AWS가 지금 상태에서 받지 않는 요청이다. 잠시 뒤 다시 시도하면 된다. */
    EC2_INSTANCE_STATE_CONFLICT(HttpStatus.CONFLICT, "EC2 인스턴스가 상태를 바꾸는 중입니다. 잠시 후 다시 시도해주세요."),

    /** AWS 호출 실패. 권한 부족(UnauthorizedOperation)도 여기에 들어가며 사유는 서버 로그에 남는다. */
    EC2_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AWS EC2 호출에 실패했습니다. 잠시 후 다시 시도해주세요."),
}
