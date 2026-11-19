package com.ogonggo.core.contentevent.persistence

import com.ogonggo.core.contentevent.domain.ContentEvent
import org.springframework.data.jpa.repository.JpaRepository

internal interface ContentEventJpaRepository : JpaRepository<ContentEvent, Long>
