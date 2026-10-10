package com.ogonggo.core.job.domain

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class JobMetricTest {

    @Test
    fun `초기 카운트는 음수일 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            JobMetric(jobId = 1L, viewCount = -1)
        }
    }
}
