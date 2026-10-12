package com.ogonggo.core.concern.persistence

import com.ogonggo.core.concern.domain.ConcernImage
import org.springframework.data.jpa.repository.JpaRepository

internal interface ConcernImageJpaRepository : JpaRepository<ConcernImage, Long> {

    fun findAllByConcernIdAndDeletedAtIsNullOrderByDisplayOrderAsc(concernId: Long): List<ConcernImage>

    fun findAllByConcernIdInAndDeletedAtIsNull(concernIds: Collection<Long>): List<ConcernImage>
}
