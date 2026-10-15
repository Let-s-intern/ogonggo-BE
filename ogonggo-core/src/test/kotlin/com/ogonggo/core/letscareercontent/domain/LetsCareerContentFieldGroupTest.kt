package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LetsCareerContentFieldGroupTest {

    @Test
    fun `다섯 직군만 추천 직군이고 합친 직군의 직무는 묶음 직군에 속한다`() {
        LetsCareerContentFieldGroup.TAGGABLE.forEach { assertEquals(it, LetsCareerContentFieldGroup.of(it)) }
        assertNull(LetsCareerContentFieldGroup.of(JobField.GAME))
        assertEquals(JobField.IT_DEVELOPMENT, JobRole.AI_DATA_ANALYST_SCIENTIST.jobField)
    }
}
