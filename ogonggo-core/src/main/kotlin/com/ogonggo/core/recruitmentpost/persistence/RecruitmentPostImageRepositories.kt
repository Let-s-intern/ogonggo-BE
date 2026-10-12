package com.ogonggo.core.recruitmentpost.persistence

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostImage
import org.springframework.data.jpa.repository.JpaRepository

internal interface RecruitmentPostImageJpaRepository : JpaRepository<RecruitmentPostImage, Long> {

    fun findAllByPostIdAndDeletedAtIsNullOrderByDisplayOrderAsc(postId: Long): List<RecruitmentPostImage>
}
