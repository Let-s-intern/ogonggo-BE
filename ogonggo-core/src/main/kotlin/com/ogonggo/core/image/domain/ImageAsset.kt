package com.ogonggo.core.image.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "image_assets")
class ImageAsset internal constructor(
    id: String,
    ownerUserId: Long,
    storageKey: String,
    url: String,
    mimeType: String,
    size: Long,
    status: ImageAssetStatus = ImageAssetStatus.UPLOADING,
) : BaseTimeEntity() {

    @Id
    @Column(length = 36)
    var id: String = id
        protected set

    @Column(name = "owner_user_id", nullable = false)
    var ownerUserId: Long = ownerUserId
        protected set

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    var storageKey: String = storageKey
        protected set

    @Column(nullable = false, length = 2048)
    var url: String = url
        protected set

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String = mimeType
        protected set

    @Column(nullable = false)
    var size: Long = size
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ImageAssetStatus = status
        protected set

    @Column(name = "unreferenced_at")
    var unreferencedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    init {
        require(id.isNotBlank()) { "이미지 식별자는 비어 있을 수 없습니다." }
        require(ownerUserId > 0) { "이미지 소유자 식별자는 양수여야 합니다." }
        require(storageKey.isNotBlank()) { "이미지 저장 키는 비어 있을 수 없습니다." }
        require(url.isNotBlank()) { "이미지 URL은 비어 있을 수 없습니다." }
        require(mimeType.isNotBlank()) { "이미지 MIME 타입은 비어 있을 수 없습니다." }
        require(size > 0) { "이미지 크기는 양수여야 합니다." }
    }

    fun markUploaded() {
        check(status == ImageAssetStatus.UPLOADING) { "업로드 중인 이미지만 완료할 수 있습니다." }
        status = ImageAssetStatus.TEMPORARY
    }

    /**
     * 본문이나 프로필에 쓰이게 된 이미지를 첨부 상태로 둔다. 어디에 쓰이는지는 쓰는 쪽이 이미지 식별자와 URL로 기억한다.
     * 이미 첨부된 이미지는 다른 곳에서 쓰는 중일 수 있으므로 다시 첨부할 수 없다.
     */
    fun attach() {
        check(deletedAt == null) { "삭제된 이미지는 연결할 수 없습니다." }
        check(isAttachable()) { "임시 또는 참조 해제된 이미지만 연결할 수 있습니다." }
        this.unreferencedAt = null
        this.status = ImageAssetStatus.ATTACHED
    }

    fun isAttachable(): Boolean =
        status == ImageAssetStatus.TEMPORARY || status == ImageAssetStatus.UNREFERENCED

    fun unreference(now: LocalDateTime) {
        if (status == ImageAssetStatus.ATTACHED) {
            unreferencedAt = now
            status = ImageAssetStatus.UNREFERENCED
        }
    }

    fun markDeletePending() {
        if (deletedAt == null && status != ImageAssetStatus.ATTACHED) {
            status = ImageAssetStatus.DELETE_PENDING
        }
    }

    fun markDeleted(now: LocalDateTime) {
        check(status == ImageAssetStatus.DELETE_PENDING) { "삭제 대기 중인 이미지만 삭제할 수 있습니다." }
        if (deletedAt == null) {
            deletedAt = now
            status = ImageAssetStatus.DELETED
        }
    }

    companion object {
        fun uploading(
            id: String,
            ownerUserId: Long,
            storageKey: String,
            url: String,
            mimeType: String,
            size: Long,
        ): ImageAsset = ImageAsset(id, ownerUserId, storageKey, url, mimeType, size)
    }
}
