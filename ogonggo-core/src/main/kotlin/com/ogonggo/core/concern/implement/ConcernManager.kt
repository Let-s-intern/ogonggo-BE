package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.implement.dto.ConcernUpdateDto
import com.ogonggo.core.concern.persistence.ConcernJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcernManager internal constructor(
    private val concernRepository: ConcernJpaRepository,
) {

    fun update(concern: Concern, dto: ConcernUpdateDto) {
        concern.update(category = dto.category, title = dto.title, content = dto.content)
        concernRepository.save(concern)
    }

    fun hide(concern: Concern) {
        concern.hide()
        concernRepository.save(concern)
    }

    fun unhide(concern: Concern) {
        concern.unhide()
        concernRepository.save(concern)
    }

    fun delete(concern: Concern, deletedAt: LocalDateTime) {
        concern.delete(deletedAt)
        concernRepository.save(concern)
    }
}
