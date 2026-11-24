package com.ogonggo.core.featureflag.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureFlagDomainTest {

    @Test
    fun `key는 소문자 snake_case여야 한다`() {
        listOf("ad_report_cost_efficiency", "flag", "flag_2", "a1_b2").forEach { key ->
            assertEquals(key, FeatureFlag(flagKey = key, enabled = true, description = "설명").flagKey)
        }

        listOf("", "AdReport", "ad-report", "ad report", "_flag", "flag_", "flag__x", "1flag", "광고").forEach { key ->
            assertThrows(IllegalArgumentException::class.java, { FeatureFlag(flagKey = key, enabled = true, description = "설명") }, key)
        }
    }

    @Test
    fun `key는 100자까지 쓸 수 있다`() {
        assertEquals(100, FeatureFlag(flagKey = "a".repeat(100), enabled = true, description = "설명").flagKey.length)
        assertThrows(IllegalArgumentException::class.java) {
            FeatureFlag(flagKey = "a".repeat(101), enabled = true, description = "설명")
        }
    }

    @Test
    fun `설명은 비어 있을 수 없고 500자를 넘을 수 없다`() {
        assertEquals(500, FeatureFlag(flagKey = "flag", enabled = true, description = "가".repeat(500)).description.length)

        assertThrows(IllegalArgumentException::class.java) { FeatureFlag(flagKey = "flag", enabled = true, description = " ") }
        assertThrows(IllegalArgumentException::class.java) { FeatureFlag(flagKey = "flag", enabled = true, description = "가".repeat(501)) }
    }

    @Test
    fun `켜짐 여부를 바꾸면 바꾼 관리자를 남기고 같은 값이어도 관리자는 갱신된다`() {
        // given
        val featureFlag = FeatureFlag(flagKey = "flag", enabled = true, description = "설명")
        assertNull(featureFlag.updatedBy)

        // when
        featureFlag.changeEnabled(enabled = false, updatedBy = 7L)

        // then
        assertFalse(featureFlag.enabled)
        assertEquals(7L, featureFlag.updatedBy)

        // when
        featureFlag.changeEnabled(enabled = false, updatedBy = 9L)
        featureFlag.changeEnabled(enabled = true, updatedBy = 9L)

        // then
        assertTrue(featureFlag.enabled)
        assertEquals(9L, featureFlag.updatedBy)
    }

    @Test
    fun `관리자 식별자는 양수여야 한다`() {
        val featureFlag = FeatureFlag(flagKey = "flag", enabled = true, description = "설명")

        assertThrows(IllegalArgumentException::class.java) { featureFlag.changeEnabled(enabled = false, updatedBy = 0L) }
        assertTrue(featureFlag.enabled)
    }
}
