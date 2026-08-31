package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "bootcamp_curriculums")
internal class BootcampCurriculum(
    @Column(name = "bootcamp_id", nullable = false)
    val bootcampId: Long, /* 커리큘럼이 속한 부트캠프 식별자 */

    @Column(name = "start_week", nullable = false)
    val startWeek: Int, /* 커리큘럼 시작 주차 */

    @Column(name = "end_week", nullable = false)
    val endWeek: Int, /* 커리큘럼 종료 주차 */

    @Column(nullable = false, length = 255)
    val subtitle: String, /* 커리큘럼 소제목 */

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0, /* 커리큘럼 노출 순서 */
) : BaseTimeEntity() {

    init {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        require(startWeek > 0) { "커리큘럼 시작 주차는 양수여야 합니다." }
        require(endWeek > 0) { "커리큘럼 종료 주차는 양수여야 합니다." }
        require(startWeek <= endWeek) { "커리큘럼 시작 주차는 종료 주차보다 클 수 없습니다." }
        require(subtitle.isNotBlank()) { "커리큘럼 소제목은 비어 있을 수 없습니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 부트캠프 커리큘럼 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 커리큘럼 삭제 일시 */
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}
