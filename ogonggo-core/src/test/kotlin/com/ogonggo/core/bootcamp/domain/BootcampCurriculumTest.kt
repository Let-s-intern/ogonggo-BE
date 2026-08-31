package com.ogonggo.core.bootcamp.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BootcampCurriculumTest {

    @Test
    fun `커리큘럼 주차와 소제목을 저장한다`() {
        val curriculum = BootcampCurriculum(
            bootcampId = 1L,
            startWeek = 1,
            endWeek = 4,
            subtitle = "Spring 기초",
            displayOrder = 0,
        )

        assertEquals(1, curriculum.startWeek)
        assertEquals(4, curriculum.endWeek)
        assertEquals("Spring 기초", curriculum.subtitle)
    }

    @Test
    fun `커리큘럼 식별자와 주차 및 노출 순서를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(bootcampId = 0) }
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(startWeek = 0) }
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(endWeek = 0) }
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(startWeek = 5, endWeek = 4) }
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(subtitle = " ") }
        assertThrows(IllegalArgumentException::class.java) { createCurriculum(displayOrder = -1) }
    }

    private fun createCurriculum(
        bootcampId: Long = 1L,
        startWeek: Int = 1,
        endWeek: Int = 4,
        subtitle: String = "Spring 기초",
        displayOrder: Int = 0,
    ): BootcampCurriculum = BootcampCurriculum(
        bootcampId = bootcampId,
        startWeek = startWeek,
        endWeek = endWeek,
        subtitle = subtitle,
        displayOrder = displayOrder,
    )
}
