package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostManager internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
) {

    fun update(post: RecruitmentPost, command: RecruitmentPostUpdateCommand) {
        post.update(
            title = command.title,
            recruitmentType = command.recruitmentType,
            capacity = command.capacity,
            progressMethod = command.progressMethod,
            activityDurationMonths = command.activityDurationMonths,
            technologyStacks = command.technologyStacks.orEmpty(),
            summary = command.summary,
            content = command.content,
            eligibilityAndSelectionProcess = command.eligibilityAndSelectionProcess,
            recruitmentStartDate = command.recruitmentStartDate,
            recruitmentEndDate = command.recruitmentEndDate,
            positions = command.positions.orEmpty(),
            contactMethod = command.contactMethod,
            contactValue = command.contactValue,
        )
        postRepository.save(post)
    }

    fun updateDraft(post: RecruitmentPost, command: RecruitmentPostUpdateCommand) {
        post.updateDraft(
            title = command.title,
            recruitmentType = command.recruitmentType,
            capacity = command.capacity,
            progressMethod = command.progressMethod,
            activityDurationMonths = command.activityDurationMonths,
            technologyStacks = command.technologyStacks.orEmpty(),
            summary = command.summary,
            content = command.content,
            eligibilityAndSelectionProcess = command.eligibilityAndSelectionProcess,
            recruitmentStartDate = command.recruitmentStartDate,
            recruitmentEndDate = command.recruitmentEndDate,
            positions = command.positions.orEmpty(),
            contactMethod = command.contactMethod,
            contactValue = command.contactValue,
        )
        postRepository.save(post)
    }

    fun copyAsDraft(post: RecruitmentPost): RecruitmentPost = postRepository.save(post.copyAsDraft())

    fun delete(post: RecruitmentPost, deletedAt: LocalDateTime) {
        post.delete(deletedAt)
        postRepository.save(post)
    }

    fun close(post: RecruitmentPost, closedAt: LocalDateTime) = change(post) { close(closedAt) }

    fun reopen(post: RecruitmentPost) = change(post) { reopen() }

    fun publish(post: RecruitmentPost) = change(post) { publish() }

    private fun change(post: RecruitmentPost, change: RecruitmentPost.() -> Unit) {
        post.change()
        postRepository.save(post)
    }
}
