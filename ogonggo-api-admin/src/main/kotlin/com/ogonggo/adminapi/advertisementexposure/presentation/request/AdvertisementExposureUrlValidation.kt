package com.ogonggo.adminapi.advertisementexposure.presentation.request

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.net.URI
import java.util.Locale
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AdvertisementImageUrlValidator::class])
annotation class ValidAdvertisementImageUrl(
    val message: String = "HTTP 또는 HTTPS 절대 URL이어야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AdvertisementLinkUrlValidator::class])
annotation class ValidAdvertisementLinkUrl(
    val message: String = "HTTP(S) URL 또는 단일 슬래시로 시작하는 내부 경로여야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

internal class AdvertisementImageUrlValidator : ConstraintValidator<ValidAdvertisementImageUrl, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value.isNullOrBlank() || value.toUriOrNull()?.isHttpUrl() == true
}

internal class AdvertisementLinkUrlValidator : ConstraintValidator<ValidAdvertisementLinkUrl, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return true
        val uri = value.toUriOrNull() ?: return false
        val isInternalRoute = value.startsWith('/') &&
            !value.startsWith("//") &&
            uri.rawAuthority == null &&
            !value.contains('\\')
        return uri.isHttpUrl() || isInternalRoute
    }
}

private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()

private fun URI.isHttpUrl(): Boolean =
    isAbsolute && scheme?.lowercase(Locale.ROOT) in setOf("http", "https") && !host.isNullOrBlank()
