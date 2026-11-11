package com.ogonggo.core.user.implement

import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * 비즈니스 회원의 임시 비밀번호 원문을 만든다. 운영자 발급과 비밀번호 찾기가 함께 쓴다.
 *
 * 메일이나 메신저로 받아 손으로 옮겨 적는 값이라 헷갈리는 글자(0·O·1·l·I)를 뺐다.
 * 영문과 숫자를 하나 이상 섞어 가입 화면의 비밀번호 길이 규칙(8~64자)을 지킨다.
 * 인코딩은 하지 않는다. core는 인코딩 방식을 모르고 각 API가 BCrypt로 인코딩한다.
 */
@Component
class CompanyTemporaryPasswordGenerator {

    private val random = SecureRandom()

    fun generate(): String {
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
