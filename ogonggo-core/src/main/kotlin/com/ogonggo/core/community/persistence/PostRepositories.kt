package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

internal interface PostJpaRepository : JpaRepository<Post, Long>, JpaSpecificationExecutor<Post>
