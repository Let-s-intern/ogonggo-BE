package com.ogonggo.userapi.auth.implement

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.userapi.auth.error.AuthErrorCode
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.Date
import javax.crypto.SecretKey

@Component
class OgonggoTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret))

    fun createAccessToken(userId: Long): String = createToken(userId, ACCESS_TYPE)

    fun createRefreshToken(userId: Long): String = createToken(userId, REFRESH_TYPE)

    fun parseAccessToken(token: String): Long = parse(token, ACCESS_TYPE, AuthErrorCode.INVALID_TOKEN)

    fun parseRefreshToken(token: String): Long = parse(token, REFRESH_TYPE, AuthErrorCode.NOT_REFRESH_TOKEN)

    private fun createToken(userId: Long, type: String): String {
        val issuedAt = clock.instant()
        val validity = when (type) {
            ACCESS_TYPE -> properties.accessTokenValidity
            else -> properties.refreshTokenValidity
        }

        return Jwts.builder()
            .subject(userId.toString())
            .claim(TYPE_CLAIM, type)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(validity)))
            .signWith(key, Jwts.SIG.HS512)
            .compact()
    }

    private fun parse(token: String, expectedType: String, typeMismatchErrorCode: AuthErrorCode): Long {
        val claims = try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (exception: JwtException) {
            throw UnauthorizedException(AuthErrorCode.INVALID_TOKEN)
        } catch (exception: IllegalArgumentException) {
            throw UnauthorizedException(AuthErrorCode.INVALID_TOKEN)
        }

        if (claims[TYPE_CLAIM] != expectedType) {
            throw UnauthorizedException(typeMismatchErrorCode)
        }

        return claims.userId()
    }

    private fun Claims.userId(): Long =
        subject?.toLongOrNull() ?: throw UnauthorizedException(AuthErrorCode.INVALID_TOKEN)

    companion object {
        private const val TYPE_CLAIM = "type"
        private const val ACCESS_TYPE = "access"
        private const val REFRESH_TYPE = "refresh"
    }
}
