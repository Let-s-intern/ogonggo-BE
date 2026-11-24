package com.ogonggo.adminapi.featureflag.business

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.featureflag.error.FeatureFlagErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-feature-flag-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminFeatureFlagServicePersistenceTest @Autowired constructor(
    private val service: AdminFeatureFlagService,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `켜짐 여부를 바꾸면 목록에 반영되고 바꾼 관리자가 요청한 관리자로 남는다`() {
        // given
        val key = insertFlag("toggle_flag_a", enabled = true)
        assertNull(find(key).updatedBy)

        // when
        service.changeFeatureFlagEnabled(adminUserId = 7L, flagKey = key, enabled = false)

        // then
        val afterOff = find(key)
        assertFalse(afterOff.enabled)
        assertEquals(7L, afterOff.updatedBy)

        // when
        service.changeFeatureFlagEnabled(adminUserId = 9L, flagKey = key, enabled = true)

        // then
        val afterOn = find(key)
        assertTrue(afterOn.enabled)
        assertEquals(9L, afterOn.updatedBy)
    }

    @Test
    fun `목록은 key 순이고 설명을 함께 준다`() {
        // given
        val keys = listOf("order_flag_c", "order_flag_a", "order_flag_b").map { insertFlag(it, enabled = false) }

        // when
        val listed = service.getFeatureFlags().filter { it.key in keys }

        // then
        assertEquals(listOf("order_flag_a", "order_flag_b", "order_flag_c"), listed.map { it.key })
        assertEquals("테스트 플래그 order_flag_a", listed.first().description)
    }

    @Test
    fun `없는 key를 바꾸면 찾을 수 없다`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            service.changeFeatureFlagEnabled(adminUserId = 7L, flagKey = "missing_flag", enabled = true)
        }

        assertEquals(FeatureFlagErrorCode.FEATURE_FLAG_NOT_FOUND, exception.errorCode)
    }

    private fun find(key: String): AdminFeatureFlagResult = service.getFeatureFlags().single { it.key == key }

    /** 운영 DB에 넣는 SQL과 같은 칼럼으로 행을 넣는다. 엔티티 생성자는 core 안에서만 쓴다. */
    private fun insertFlag(key: String, enabled: Boolean): String {
        jdbcTemplate.update(
            """
            insert into feature_flags (flag_key, enabled, description, created_at, updated_at)
            values (?, ?, ?, current_timestamp, current_timestamp)
            """,
            key,
            enabled,
            "테스트 플래그 $key",
        )
        return key
    }
}
