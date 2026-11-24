package com.ogonggo.core.featureflag.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.featureflag.domain.FeatureFlag
import com.ogonggo.core.featureflag.error.FeatureFlagErrorCode
import com.ogonggo.core.featureflag.persistence.FeatureFlagJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(FeatureFlagReader::class, FeatureFlagManager::class)
internal class FeatureFlagPersistenceTest @Autowired constructor(
    private val featureFlagReader: FeatureFlagReader,
    private val featureFlagManager: FeatureFlagManager,
    private val featureFlagRepository: FeatureFlagJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `key로 플래그를 찾는다`() {
        // given
        featureFlagRepository.saveAndFlush(FeatureFlag("ad_report_cost_efficiency", true, "비용 효율 섹션을 보인다"))
        featureFlagRepository.saveAndFlush(FeatureFlag("other_flag", false, "다른 플래그"))
        entityManager.clear()

        // when
        val found = featureFlagReader.read("ad_report_cost_efficiency")

        // then
        assertEquals("ad_report_cost_efficiency", found.flagKey)
        assertTrue(found.enabled)
        assertEquals("비용 효율 섹션을 보인다", found.description)
        assertNull(found.updatedBy)
    }

    @Test
    fun `없는 key는 찾을 수 없다`() {
        featureFlagRepository.saveAndFlush(FeatureFlag("ad_report_cost_efficiency", true, "설명"))

        val exception = assertThrows(EntityNotFoundException::class.java) { featureFlagReader.read("missing_flag") }

        assertEquals(FeatureFlagErrorCode.FEATURE_FLAG_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `목록은 key 순이다`() {
        // given
        featureFlagRepository.saveAndFlush(FeatureFlag("c_flag", true, "설명"))
        featureFlagRepository.saveAndFlush(FeatureFlag("a_flag", false, "설명"))
        featureFlagRepository.saveAndFlush(FeatureFlag("b_flag", true, "설명"))

        // when & then
        assertEquals(listOf("a_flag", "b_flag", "c_flag"), featureFlagReader.readAll().map { it.flagKey })
    }

    @Test
    fun `켜짐 여부를 바꾸면 저장되고 바꾼 관리자가 남는다`() {
        // given
        featureFlagRepository.saveAndFlush(FeatureFlag("flag", true, "설명"))
        entityManager.clear()

        // when
        featureFlagManager.changeEnabled(featureFlagReader.read("flag"), enabled = false, updatedBy = 7L)
        entityManager.flush()
        entityManager.clear()

        // then
        val reloaded = featureFlagReader.read("flag")
        assertFalse(reloaded.enabled)
        assertEquals(7L, reloaded.updatedBy)
    }

    @Test
    fun `같은 key는 두 번 저장할 수 없다`() {
        featureFlagRepository.saveAndFlush(FeatureFlag("flag", true, "설명"))

        assertThrows(DataIntegrityViolationException::class.java) {
            featureFlagRepository.saveAndFlush(FeatureFlag("flag", false, "다른 설명"))
        }
    }
}
