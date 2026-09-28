package com.ogonggo.core.enumeration

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnumFieldTest {

    @Test
    fun `업무 enum은 양수의 고유 코드와 설명을 가진다`() {
        val enumTypes = listOf(
            EmploymentType.entries,
            ExperienceType.entries,
            EducationLevel.entries,
            JobRecruitmentType.entries,
            JobPublicationStatus.entries,
            ApplicationMethod.entries,
            BootcampStatus.entries,
            BootcampRecruitmentType.entries,
            OperationType.entries,
            TuitionType.entries,
            UserStatus.entries,
            UserRole.entries,
        )

        enumTypes.forEach { values ->
            assertTrue(values.all { it.code > 0 })
            assertTrue(values.all { it.desc.isNotBlank() })
            assertEquals(values.size, values.map { it.code }.distinct().size)
        }
    }
}
