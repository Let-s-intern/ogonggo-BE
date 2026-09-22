package com.ogonggo.adminapi.config

import com.ogonggo.core.enumeration.EnumField
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.Schema
import org.springframework.stereotype.Component

/**
 * `EnumField`를 구현한 enum의 스키마 설명에 값 목록을 채운다.
 *
 * 기본 동작은 상수 이름만 내보내므로 문서만 보고는 각 값이 무슨 뜻인지, code가 몇 번인지 알 수 없다.
 * enum마다 `@Schema(description = ...)`을 직접 적으면 값이 늘어날 때 문서가 코드와 어긋나므로
 * 여기서 한 번에 만들어 붙인다.
 *
 * 필드에 `@Schema(allowableValues = ...)`로 받을 값을 좁히면 기본 동작은 전체 상수 뒤에 그 값을 덧붙이므로,
 * 값 목록과 설명 표를 좁힌 값으로 맞춘다.
 */
@Component
class EnumFieldModelConverter : ModelConverter {

    override fun resolve(
        type: AnnotatedType,
        context: ModelConverterContext,
        chain: MutableIterator<ModelConverter>,
    ): Schema<*>? {
        val schema = if (chain.hasNext()) chain.next().resolve(type, context, chain) else null
        val enumClass = enumFieldClassOf(type) ?: return schema
        val allowedNames = allowedNamesOf(type)
        return schema?.apply {
            if (allowedNames.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                (this as Schema<Any>).enum = allowedNames.toList()
            }
            description = describe(enumClass, allowedNames, description)
        }
    }

    private fun allowedNamesOf(type: AnnotatedType): Set<String> =
        type.ctxAnnotations.orEmpty()
            .filterIsInstance<io.swagger.v3.oas.annotations.media.Schema>()
            .flatMap { it.allowableValues.asList() }
            .toCollection(LinkedHashSet())

    private fun enumFieldClassOf(type: AnnotatedType): Class<*>? {
        val rawClass = runCatching { Json.mapper().constructType(type.type)?.rawClass }.getOrNull()
        return rawClass?.takeIf { it.isEnum && EnumField::class.java.isAssignableFrom(it) }
    }

    /** 기존 설명이 있으면 지우지 않고 표를 덧붙인다. */
    private fun describe(enumClass: Class<*>, allowedNames: Set<String>, existingDescription: String?): String {
        val rows = enumClass.enumConstants
            .filterIsInstance<EnumField>()
            .filter { allowedNames.isEmpty() || (it as Enum<*>).name in allowedNames }
            .joinToString("\n") { value ->
                "| `${(value as Enum<*>).name}` | ${value.code} | ${value.desc} |"
            }
        val table = "| 값 | code | 설명 |\n| --- | --- | --- |\n$rows"
        return listOfNotNull(existingDescription?.takeIf(String::isNotBlank), table).joinToString("\n\n")
    }
}
