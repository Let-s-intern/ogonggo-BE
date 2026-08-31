package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "bootcamp_partners",
    uniqueConstraints = [UniqueConstraint(name = "uk_bootcamp_partner", columnNames = ["bootcamp_id", "partner_name"])],
)
internal class BootcampPartner(
    @Column(name = "bootcamp_id", nullable = false)
    val bootcampId: Long, /* 파트너사가 참여하는 부트캠프 식별자 */

    @Column(name = "partner_name", nullable = false, length = 150)
    val partnerName: String, /* 파트너사명 */

    displayOrder: Int = 0, /* 파트너사 노출 순서 */
) : BaseTimeEntity() {

    init {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        require(partnerName.isNotBlank()) { "파트너사명은 비어 있을 수 없습니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 부트캠프 파트너사 식별자 */
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder /* 파트너사 노출 순서 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 파트너사 연결 삭제 일시 */
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    fun restore(displayOrder: Int) {
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
        this.displayOrder = displayOrder
        deletedAt = null
    }
}
