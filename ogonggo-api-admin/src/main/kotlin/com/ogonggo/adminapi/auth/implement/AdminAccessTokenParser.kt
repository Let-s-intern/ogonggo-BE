package com.ogonggo.adminapi.auth.implement

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey

/**
 * 사용자 API가 발급한 액세스 토큰에서 사용자 식별자를 읽는다. 관리자 API는 토큰을 발급하지 않는다.
 * 서명·만료·종류 중 하나라도 맞지 않으면 인증하지 않은 것으로 보고 null을 돌려준다.
 */
class AdminAccessTokenParser(
    properties: AdminJwtProperties,
) {
    private val key: SecretKey? = properties.secret
        .takeIf { it.isNotBlank() }
        ?.let { Keys.hmacShaKeyFor(Decoders.BASE64.decode(it)) }

    fun parseUserId(token: String): Long? {
        val verifiedKey = key ?: return null
        val claims = try {
            Jwts.parser()
                .verifyWith(verifiedKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (exception: JwtException) {
            return null
        } catch (exception: IllegalArgumentException) {
            return null
        }

        // 리프레시 토큰으로 API를 호출하지 못하도록 종류를 확인한다.
        if (claims[TYPE_CLAIM] != ACCESS_TYPE) {
            return null
        }
        return claims.subject?.toLongOrNull()
    }

    companion object {
        /** 사용자 API의 `OgonggoTokenProvider`가 쓰는 클레임과 같아야 한다. */
        private const val TYPE_CLAIM = "type"
        private const val ACCESS_TYPE = "access"
    }
}
