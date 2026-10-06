package com.ogonggo.core.enumeration.catalog

import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.job.domain.JobRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

class EnumOptionReaderTest {

    private val enums = EnumOptionReader().readAll()

    @Test
    fun `enum 값을 선언 순서대로 이름과 라벨, 상위 값으로 제공한다`() {
        assertEquals(
            JobRole.entries.map { Triple(it.name, it.desc, it.parent.name) },
            enums.getValue("JobRole").map { Triple(it.name, it.desc, it.parent) },
        )
    }

    /** core에 업무 enum을 새로 만들고 목록에 빠뜨리면 두 API 모두 그 값의 라벨을 내려 주지 못한다. */
    @Test
    fun `core의 업무 enum을 모두 제공한다`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AssignableTypeFilter(EnumField::class.java))
        }
        val coreEnums = scanner.findCandidateComponents("com.ogonggo.core")
            .map { Class.forName(it.beanClassName) }
            .filter { it.isEnum }
            .map { it.simpleName }
            .toSet()

        val missing = coreEnums - enums.keys
        assertTrue(coreEnums.isNotEmpty())
        assertTrue(missing.isEmpty(), "선택지 목록에 없는 enum: $missing")
    }
}
