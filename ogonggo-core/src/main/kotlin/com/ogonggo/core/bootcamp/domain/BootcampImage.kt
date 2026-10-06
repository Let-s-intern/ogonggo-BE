package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 부트캠프 상세에서만 보여 주는 사진이다. 목록에 쓰는 대표 이미지(`representative_image_url`)와 따로 둔다.
 * 고용24에서 수집한 과정의 훈련기관 시설 사진이 들어온다.
 */
@Entity
@Table(name = "bootcamp_images")
internal class BootcampImage(
    @Column(name = "bootcamp_id", nullable = false)
    val bootcampId: Long, /* 사진이 속한 부트캠프 식별자 */

    @Column(name = "image_url", nullable = false, length = 2048)
    val imageUrl: String, /* 사진 주소 */

    @Column(length = 255)
    val caption: String? = null, /* 사진 설명 */

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0, /* 사진 노출 순서 */
) : BaseTimeEntity() {

    init {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        require(imageUrl.isNotBlank()) { "사진 주소는 비어 있을 수 없습니다." }
        require(caption == null || caption.isNotBlank()) { "사진 설명은 공백일 수 없습니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 부트캠프 사진 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 사진 삭제 일시 */
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}
