package com.ogonggo.core.enumeration

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
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
            JobEmploymentType.entries,
            JobExperienceType.entries,
            JobEducationLevel.entries,
            JobRecruitmentType.entries,
            JobPublicationStatus.entries,
            BootcampApplicationMethod.entries,
            BootcampRecruitmentStatus.entries,
            BootcampRecruitmentType.entries,
            BootcampOperationType.entries,
            BootcampTuitionType.entries,
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
