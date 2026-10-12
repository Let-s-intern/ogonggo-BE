package com.ogonggo.core.recruitmentpost.domain

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
 * 모집글에 첨부한 사진이다. 본문 밖에 따로 두며 모집글마다 최대 [MAX_COUNT_PER_POST]장이다.
 * 이미지(`image_assets`)는 어디에 쓰이는지 모르므로, 모집글 쪽이 이미지 식별자와 URL을 함께 기억한다.
 */
@Entity
@Table(
    name = "recruitment_post_images",
    indexes = [Index(name = "idx_recruitment_post_image_post", columnList = "post_id, deleted_at")],
)
class RecruitmentPostImage internal constructor(
    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "image_id", nullable = false, length = 36)
    val imageId: String, /* 이미지 식별자(image_assets.id) */

    @Column(name = "image_url", nullable = false, length = 2048)
    val imageUrl: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
) : BaseTimeEntity() {

    init {
        require(postId > 0) { "모집글 식별자는 양수여야 합니다." }
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

    companion object {
        const val MAX_COUNT_PER_POST = 3
    }
}
