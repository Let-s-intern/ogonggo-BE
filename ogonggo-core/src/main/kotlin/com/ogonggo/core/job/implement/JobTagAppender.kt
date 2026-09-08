package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobTag
import com.ogonggo.core.job.domain.Tag
import com.ogonggo.core.job.persistence.JobTagJpaRepository
import com.ogonggo.core.job.persistence.TagJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 태그 생성만 호출자와 분리된 트랜잭션에서 처리한다.
 *
 * 같은 태그를 동시에 만들면 태그명 유니크 제약에 걸리는데, 그 실패가 호출자의 트랜잭션까지
 * 롤백 대상으로 만들면 태그 하나 때문에 공고 등록 전체가 함께 실패한다.
 * 태그는 공고와 독립적인 마스터 데이터이므로 따로 커밋해도 무결성이 깨지지 않는다.
 */
@Component
internal class TagRegistrar(
    private val tagRepository: TagJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(name: String): Long =
        checkNotNull(tagRepository.saveAndFlush(Tag(name = name)).id) { "저장된 태그 식별자가 없습니다." }
}

@Component
class JobTagAppender internal constructor(
    private val tagRepository: TagJpaRepository,
    private val jobTagRepository: JobTagJpaRepository,
    private val tagRegistrar: TagRegistrar,
) {

    /**

    * 태그 이름을 정규화해 중복을 제거한 뒤 공고에 연결한다.

    * 없는 태그는 새로 만들고 이미 있는 태그는 재사용한다.

    */

    fun append(jobId: Long, tagNames: Collection<String>) {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }

        val names = tagNames.map(::normalize).filter(String::isNotBlank).distinct()
        if (names.isEmpty()) {
            return
        }

        val tagIds = names.map(::readOrCreateTagId)
        val linkedTagIds = jobTagRepository.findAllByJobId(jobId).mapTo(mutableSetOf(), JobTag::tagId)

        jobTagRepository.saveAll(
            tagIds.filterNot(linkedTagIds::contains).map { tagId -> JobTag(jobId = jobId, tagId = tagId) },
        )
    }

    /**
     * 태그명에는 유니크 제약이 있어 같은 태그를 동시에 만들면 한쪽이 실패한다.
     * 실패는 생성 전용 트랜잭션 안에서만 일어나므로 여기서 상대가 만든 태그를 다시 읽어 사용한다.
     */
    private fun readOrCreateTagId(name: String): Long =
        findTagId(name)
            ?: try {
                tagRegistrar.create(name)
            } catch (exception: DataIntegrityViolationException) {
                findTagId(name) ?: throw exception
            }

    private fun findTagId(name: String): Long? =
        tagRepository.findAllByNameIn(listOf(name)).firstOrNull()?.id
}

/** 표기 차이로 같은 태그가 여러 개 생기지 않도록 앞뒤 공백과 연속 공백을 정리한다. */
private fun normalize(name: String): String = name.trim().replace(WHITESPACE, " ")

private val WHITESPACE = Regex("\\s+")
