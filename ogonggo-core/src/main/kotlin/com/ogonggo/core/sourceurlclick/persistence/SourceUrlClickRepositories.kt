package com.ogonggo.core.sourceurlclick.persistence

import com.ogonggo.core.sourceurlclick.domain.SourceUrlClick
import com.ogonggo.core.sourceurlclick.domain.SourceUrlClickTargetType
import org.springframework.data.jpa.repository.JpaRepository

internal interface SourceUrlClickJpaRepository : JpaRepository<SourceUrlClick, Long> {
    fun existsByTargetTypeAndTargetIdAndUserId(targetType: SourceUrlClickTargetType, targetId: Long, userId: Long): Boolean
}
