package com.ogonggo.adminapi.enumeration.business

import com.ogonggo.adminapi.ingestion.work24.implement.Work24Api
import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.job.domain.JobRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.RegexPatternTypeFilter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Pattern

class AdminEnumServiceTest {

    private val enums = AdminEnumService().getEnums()

    @Test
    fun `enum 값을 선언 순서대로 이름과 라벨, 상위 값으로 제공한다`() {
        val jobRoles = enums.getValue("JobRole")

        assertEquals(
            JobRole.entries.map { Triple(it.name, it.desc, it.parent.name) },
            jobRoles.map { Triple(it.name, it.desc, it.parent) },
        )
    }

    /**
     * 관리자 API의 요청·응답에 새 업무 enum을 추가하고 이 목록을 빠뜨리면
     * 관리자 콘솔은 그 값의 라벨을 받을 수 없다. presentation의 요청·응답 필드와 Controller 파라미터를 훑어 막는다.
     */
    @Test
    fun `관리자 API 요청·응답에 쓰는 업무 enum을 모두 제공한다`() {
        val missing = (adminApiPresentationEnumTypes() - NOT_OPTIONS).map { it.simpleName }.toSet() - enums.keys

        assertTrue(missing.isEmpty(), "선택지 목록에 없는 enum: $missing")
    }

    private fun adminApiPresentationEnumTypes(): Set<Class<*>> {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean = true
        }
        scanner.addIncludeFilter(RegexPatternTypeFilter(Pattern.compile(".*\\.presentation\\..*")))

        return scanner.findCandidateComponents("com.ogonggo.adminapi")
            .map { Class.forName(it.beanClassName) }
            .flatMap { type ->
                type.declaredFields.map { it.genericType } +
                    type.declaredMethods.flatMap { it.genericParameterTypes.asList() }
            }
            .flatMap(::rawTypes)
            .filter { it.isEnum && EnumField::class.java.isAssignableFrom(it) }
            .toSet()
    }

    private companion object {
        /**
         * 고용24 응답 조회 경로는 enum 이름이 아니라 kebab-case 소문자(`recruitments`)를 받으므로
         * 이름을 선택지로 내보내면 클라이언트가 잘못된 값을 보내게 된다.
         */
        val NOT_OPTIONS: Set<Class<*>> = setOf(Work24Api::class.java)
    }

    /** `List<RecruitmentPostPosition>`처럼 컬렉션에 담긴 enum도 찾도록 타입 인자까지 푼다. */
    private fun rawTypes(type: Type): List<Class<*>> = when (type) {
        is Class<*> -> listOf(type)
        is ParameterizedType -> rawTypes(type.rawType) + type.actualTypeArguments.flatMap(::rawTypes)
        else -> emptyList()
    }
}
