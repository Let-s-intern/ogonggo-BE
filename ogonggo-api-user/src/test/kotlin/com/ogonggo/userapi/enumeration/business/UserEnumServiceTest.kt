package com.ogonggo.userapi.enumeration.business

import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.enumeration.catalog.EnumOptionReader
import com.ogonggo.core.job.domain.EmploymentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.RegexPatternTypeFilter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Pattern

class UserEnumServiceTest {

    private val enums = UserEnumService(EnumOptionReader()).getEnums()

    @Test
    fun `enum 값을 선언 순서대로 이름과 라벨로 제공한다`() {
        val employmentTypes = enums.getValue("EmploymentType")

        assertEquals(
            EmploymentType.entries.map { it.name to it.desc },
            employmentTypes.map { it.name to it.desc },
        )
    }

    @Test
    fun `프런트 전환 기간에는 모집글 선택지를 예전 키로도 같은 값으로 제공한다`() {
        assertEquals(enums.getValue("RecruitmentPostType"), enums.getValue("RecruitmentType"))
        assertEquals(enums.getValue("RecruitmentPostPosition"), enums.getValue("RecruitmentPosition"))
        assertEquals(
            enums.getValue("RecruitmentPostApplicationSortType"),
            enums.getValue("RecruitmentApplicationSortType"),
        )
    }

    /**
     * 사용자 API의 요청·응답에 새 업무 enum을 추가하고 이 목록을 빠뜨리면
     * 프론트는 그 값의 라벨을 받을 수 없다. presentation의 요청·응답 필드와 Controller 파라미터를 훑어 막는다.
     */
    @Test
    fun `사용자 API 요청·응답에 쓰는 업무 enum을 모두 제공한다`() {
        val missing = userApiPresentationEnumTypes().map { it.simpleName }.toSet() - enums.keys

        assertTrue(missing.isEmpty(), "선택지 목록에 없는 enum: $missing")
    }

    private fun userApiPresentationEnumTypes(): Set<Class<*>> {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean = true
        }
        scanner.addIncludeFilter(RegexPatternTypeFilter(Pattern.compile(".*\\.presentation\\..*")))

        return scanner.findCandidateComponents("com.ogonggo.userapi")
            .map { Class.forName(it.beanClassName) }
            .flatMap { type ->
                type.declaredFields.map { it.genericType } +
                    type.declaredMethods.flatMap { it.genericParameterTypes.asList() }
            }
            .flatMap(::rawTypes)
            .filter { it.isEnum && EnumField::class.java.isAssignableFrom(it) }
            .toSet()
    }

    /** `List<RecruitmentPostPosition>`처럼 컬렉션에 담긴 enum도 찾도록 타입 인자까지 푼다. */
    private fun rawTypes(type: Type): List<Class<*>> = when (type) {
        is Class<*> -> listOf(type)
        is ParameterizedType -> rawTypes(type.rawType) + type.actualTypeArguments.flatMap(::rawTypes)
        else -> emptyList()
    }
}
