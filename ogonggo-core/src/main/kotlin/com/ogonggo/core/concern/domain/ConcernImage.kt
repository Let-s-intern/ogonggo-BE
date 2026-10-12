package com.ogonggo.core.concern.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 고민글에 첨부한 사진이다. 본문 밖에 따로 두며 고민글마다 최대 [Concern.IMAGE_MAX_COUNT]장이다.
 * 이미지(`image_assets`)는 어디에 쓰이는지 모르므로, 고민글 쪽이 이미지 식별자와 URL을 함께 기억한다.
 */
@Entity
@Table(
    name = "concern_images",
    indexes = [Index(name = "idx_concern_image_concern", columnList = "concern_id, deleted_at")],
)
internal class ConcernImage(
    @Column(name = "concern_id", nullable = false)
    val concernId: Long,

    @Column(name = "image_id", nullable = false, length = 36)
    val imageId: String, /* 이미지 식별자(image_assets.id) */

    @Column(name = "image_url", nullable = false, length = 2048)
    val imageUrl: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
) : BaseTimeEntity() {

    init {
        require(concernId > 0) { "고민글 식별자는 양수여야 합니다." }
        require(imageId.isNotBlank()) { "이미지 식별자는 비어 있을 수 없습니다." }
        require(imageUrl.isNotBlank()) { "사진 주소는 비어 있을 수 없습니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}
