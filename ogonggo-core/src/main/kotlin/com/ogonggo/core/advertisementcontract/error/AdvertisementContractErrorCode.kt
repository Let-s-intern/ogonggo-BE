package com.ogonggo.core.advertisementcontract.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementContractErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISEMENT_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "광고 계약을 찾을 수 없습니다."),
    ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "계약에 붙인 광고 상품을 찾을 수 없습니다."),
    ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW(HttpStatus.CONFLICT, "비즈니스 공고 계약의 상태는 공고 검수로만 바뀝니다."),
    BUSINESS_ACCOUNT_NOT_ISSUABLE(HttpStatus.CONFLICT, "비회원이 광고 신청 폼으로 낸 계약에만 비즈니스 계정을 발급할 수 있습니다."),
    BUSINESS_ACCOUNT_MEMBER_NOT_ACTIVE(HttpStatus.CONFLICT, "신청 이메일로 가입한 비즈니스 회원이 정지·탈퇴 상태라 연결할 수 없습니다."),
}
