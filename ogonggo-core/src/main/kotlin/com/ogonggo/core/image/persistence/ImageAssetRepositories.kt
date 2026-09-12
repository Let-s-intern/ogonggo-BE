package com.ogonggo.core.image.persistence

import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

internal interface ImageAssetJpaRepository : JpaRepository<ImageAsset, String> {
    fun findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(
        ids: Collection<String>,
        ownerUserId: Long,
    ): List<ImageAsset>

    fun findAllByPostIdAndStatusAndDeletedAtIsNull(
        postId: Long,
        status: ImageAssetStatus,
    ): List<ImageAsset>

    fun findAllByStatusInAndDeletedAtIsNullAndCreatedAtBefore(
        statuses: Collection<ImageAssetStatus>,
        createdAt: LocalDateTime,
        pageable: Pageable,
    ): List<ImageAsset>

    fun findAllByStatusAndDeletedAtIsNullAndUnreferencedAtBefore(
        status: ImageAssetStatus,
        unreferencedAt: LocalDateTime,
        pageable: Pageable,
    ): List<ImageAsset>

    fun findAllByStatusAndDeletedAtIsNull(
        status: ImageAssetStatus,
        pageable: Pageable,
    ): List<ImageAsset>
}
