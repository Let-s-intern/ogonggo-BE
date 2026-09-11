package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.persistence.PostJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface PostManager {
    fun update(post: Post, command: PostUpdateCommand)

    fun delete(post: Post, deletedAt: LocalDateTime)
}

@Component
internal class PostManagerImpl(
    private val postRepository: PostJpaRepository,
) : PostManager {

    override fun update(post: Post, command: PostUpdateCommand) {
        post.update(
            title = command.title,
            recruitmentType = command.recruitmentType,
            capacity = command.capacity,
            progressMethod = command.progressMethod,
            activityDurationMonths = command.activityDurationMonths,
            technologyStacks = command.technologyStacks,
            summary = command.summary,
            content = command.content,
            eligibilityAndSelectionProcess = command.eligibilityAndSelectionProcess,
            recruitmentStartDate = command.recruitmentStartDate,
            recruitmentEndDate = command.recruitmentEndDate,
            positions = command.positions,
            contactMethod = command.contactMethod,
            contactValue = command.contactValue,
        )
        postRepository.save(post)
    }

    override fun delete(post: Post, deletedAt: LocalDateTime) {
        post.delete(deletedAt)
        postRepository.save(post)
    }
}
