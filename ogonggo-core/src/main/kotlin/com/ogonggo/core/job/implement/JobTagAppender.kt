package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobTag
import com.ogonggo.core.job.domain.Tag
import com.ogonggo.core.job.persistence.JobTagJpaRepository
import com.ogonggo.core.job.persistence.TagJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

interface JobTagAppender {
    /**
     * 태그 이름을 정규화해 중복을 제거한 뒤 공고에 연결한다.
     * 없는 태그는 새로 만들고 이미 있는 태그는 재사용한다.
     */
    fun append(jobId: Long, tagNames: Collection<String>)
}

@Component
internal class JobTagAppenderImpl(
    private val tagRepository: TagJpaRepository,
    private val jobTagRepository: JobTagJpaRepository,
) : JobTagAppender {

    override fun append(jobId: Long, tagNames: Collection<String>) {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }

        val names = tagNames.map(::normalize).filter(String::isNotBlank).distinct()
        if (names.isEmpty()) {
            return
        }

        val tagIds = names.map { name -> requiredId(readOrCreateTag(name)) }
        val linkedTagIds = jobTagRepository.findAllByJobId(jobId).mapTo(mutableSetOf(), JobTag::tagId)

        jobTagRepository.saveAll(
            tagIds.filterNot(linkedTagIds::contains).map { tagId -> JobTag(jobId = jobId, tagId = tagId) },
        )
    }

    /**
     * 태그명에는 유니크 제약이 있어 같은 태그를 동시에 만들면 한쪽이 실패한다.
     * 실패한 쪽은 상대가 만든 태그를 다시 읽어 사용한다.
     */
    private fun readOrCreateTag(name: String): Tag =
        tagRepository.findAllByNameIn(listOf(name)).firstOrNull()
            ?: try {
                tagRepository.saveAndFlush(Tag(name = name))
            } catch (exception: DataIntegrityViolationException) {
                tagRepository.findAllByNameIn(listOf(name)).firstOrNull()
                    ?: throw exception
            }

    private fun requiredId(tag: Tag): Long = checkNotNull(tag.id) { "저장된 태그 식별자가 없습니다." }
}

/** 표기 차이로 같은 태그가 여러 개 생기지 않도록 앞뒤 공백과 연속 공백을 정리한다. */
private fun normalize(name: String): String = name.trim().replace(WHITESPACE, " ")

private val WHITESPACE = Regex("\\s+")
