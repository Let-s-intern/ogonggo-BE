package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.persistence.PostJpaRepository
import org.springframework.stereotype.Component

interface PostAppender {
    fun append(command: PostAppendCommand): Post
}

@Component
internal class PostAppenderImpl(
    private val postRepository: PostJpaRepository,
) : PostAppender {

    override fun append(command: PostAppendCommand): Post = postRepository.save(command.toEntity())
}
