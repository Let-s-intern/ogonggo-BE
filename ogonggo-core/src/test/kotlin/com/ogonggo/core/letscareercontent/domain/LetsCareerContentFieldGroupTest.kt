package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.job.domain.JobField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LetsCareerContentFieldGroupTest {

    @Test
    fun `다섯 직군은 그대로이고 정한 직군은 묶음으로 옮기며 나머지는 묶음이 없다`() {
        LetsCareerContentFieldGroup.TAGGABLE.forEach { assertEquals(it, LetsCareerContentFieldGroup.of(it)) }
        assertEquals(JobField.IT_DEVELOPMENT, LetsCareerContentFieldGroup.of(JobField.AI_DATA))
        assertEquals(JobField.PLANNING_STRATEGY, LetsCareerContentFieldGroup.of(JobField.MERCHANDISING))
        assertEquals(JobField.PLANNING_STRATEGY, LetsCareerContentFieldGroup.of(JobField.DESIGN))
        assertEquals(JobField.HR_GENERAL_AFFAIRS, LetsCareerContentFieldGroup.of(JobField.ACCOUNTING_TAX_FINANCE))
        assertEquals(JobField.HR_GENERAL_AFFAIRS, LetsCareerContentFieldGroup.of(JobField.CUSTOMER_SERVICE_TM))
        assertEquals(JobField.MARKETING_ADVERTISING, LetsCareerContentFieldGroup.of(JobField.MEDIA_ENTERTAINMENT))
        assertNull(LetsCareerContentFieldGroup.of(JobField.GAME))
    }
}
