package com.ogonggo.adminapi.advertisementcontract.implement

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * 운영자가 발급하는 비즈니스 계정의 임시 비밀번호를 만든다.
 *
 * 메일로 받아 손으로 옮겨 적는 값이라 헷갈리는 글자(0·O·1·l·I)를 뺐다. 영문과 숫자를 하나 이상 섞어
 * 가입 화면의 비밀번호 길이 규칙(8~64자)을 지킨다.
 * 인코딩은 사용자 API의 로그인과 같은 BCrypt다. 로그인은 사용자 API가 이 값으로 대조한다.
 */
@Component
class CompanyTemporaryPasswordIssuer {

    private val random = SecureRandom()
    private val passwordEncoder = BCryptPasswordEncoder()

    fun issue(): CompanyTemporaryPassword {
        val raw = generate()
        return CompanyTemporaryPassword(raw = raw, encoded = checkNotNull(passwordEncoder.encode(raw)))
    }

    private fun generate(): String {
        while (true) {
            val candidate = (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
            if (candidate.any(Char::isLetter) && candidate.any(Char::isDigit)) {
                return candidate
            }
        }
    }

    private companion object {
        const val LENGTH = 12
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    }
}

/** [raw]는 메일로만 보내고 저장·로그에 남기지 않는다. */
class CompanyTemporaryPassword(
    val raw: String,
    val encoded: String,
) {
    override fun toString(): String = "CompanyTemporaryPassword(raw=****)"
}
