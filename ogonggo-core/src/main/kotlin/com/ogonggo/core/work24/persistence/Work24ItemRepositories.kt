package com.ogonggo.core.work24.persistence

import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.domain.Work24Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface Work24ItemJpaRepository : JpaRepository<Work24Item, Long> {

    @Query(
        "select item.externalId from Work24Item item " +
            "where item.api = :api and item.externalId in :externalIds",
    )
    fun findExternalIds(
        @Param("api") api: Work24Api,
        @Param("externalIds") externalIds: Collection<String>,
    ): List<String>
}
