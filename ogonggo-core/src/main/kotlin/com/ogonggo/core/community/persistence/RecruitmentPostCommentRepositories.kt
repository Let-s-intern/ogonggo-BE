package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPostComment
import org.springframework.data.jpa.repository.JpaRepository

internal interface RecruitmentPostCommentJpaRepository : JpaRepository<RecruitmentPostComment, Long>
