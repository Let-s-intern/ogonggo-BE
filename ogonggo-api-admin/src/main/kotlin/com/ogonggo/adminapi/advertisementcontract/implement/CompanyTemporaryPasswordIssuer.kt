package com.ogonggo.adminapi.advertisementcontract.implement

import com.ogonggo.core.user.implement.CompanyTemporaryPasswordGenerator
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

/**
 * 운영자가 발급하는 비즈니스 계정의 임시 비밀번호를 만들고 인코딩한다.
 * 인코딩은 사용자 API의 로그인과 같은 BCrypt다. 로그인은 사용자 API가 이 값으로 대조한다.
 */
@Component
class CompanyTemporaryPasswordIssuer(
    private val generator: CompanyTemporaryPasswordGenerator,
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    fun issue(): CompanyTemporaryPassword {
        val raw = generator.generate()
        return CompanyTemporaryPassword(raw = raw, encoded = checkNotNull(passwordEncoder.encode(raw)))
    }
}

/** [raw]는 메일과 운영자 응답으로만 내보내고 저장·로그에 남기지 않는다. */
class CompanyTemporaryPassword(
    val raw: String,
    val encoded: String,
) {
    override fun toString(): String = "CompanyTemporaryPassword(raw=****)"
}
