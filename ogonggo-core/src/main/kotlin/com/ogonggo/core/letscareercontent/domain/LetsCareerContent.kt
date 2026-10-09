package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * 렛츠커리어 콘텐츠(프로그램·무료 자료집·블로그)의 사본이다. 공고 상세에서 그 공고에 맞는 콘텐츠를 추천하려고 둔다.
 *
 * 원본은 렛츠커리어가 소유한다. 오공고는 렛츠커리어 내부 API의 목록으로 주기적으로 덮어쓰고,
 * 목록에서 빠진 콘텐츠(모집 마감, 비노출)는 지운 것으로 표시한다. 다시 목록에 나오면 되살린다.
 *
 * 추천용 태그는 크롤러가 AI로 붙인다. 태그를 붙일 때의 [contentHash]를 함께 남겨,
 * 제목·설명이 바뀐 콘텐츠를 다시 태그할 대상으로 고른다. 태그가 없으면 추천하지 않는다.
 */
@Entity
@Table(
    name = "lets_career_contents",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_lets_career_contents_source", columnNames = ["kind", "external_id"]),
    ],
)
class LetsCareerContent internal constructor(
    kind: LetsCareerContentKind,
    externalId: Long,
    source: LetsCareerContentSource,
) : BaseTimeEntity() {

    init {
        require(externalId > 0) { "렛츠커리어 콘텐츠 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 렛츠커리어 콘텐츠 사본 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    val kind: LetsCareerContentKind = kind /* 콘텐츠 종류 */

    @Column(name = "external_id", nullable = false)
    val externalId: Long = externalId /* 렛츠커리어의 콘텐츠 식별자 */

    @Column(name = "category", length = 100)
    var category: String? = null /* 렛츠커리어의 분류 enum 이름(챌린지 유형, 블로그 카테고리 등) */
        protected set

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    var title: String = "" /* 제목 */
        protected set

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null /* 짧은 설명 */
        protected set

    @Column(name = "thumbnail_url", length = MAX_URL_LENGTH)
    var thumbnailUrl: String? = null /* 썸네일 이미지 주소 */
        protected set

    @Column(name = "path", nullable = false, length = MAX_URL_LENGTH)
    var path: String = "" /* 렛츠커리어 웹 상세 경로 */
        protected set

    @Column(name = "labels", columnDefinition = "TEXT")
    var labels: String? = null /* 직무·추천 대상·해시태그 같은 분류 단서, 줄바꿈으로 구분 */
        protected set

    @Column(name = "recruitment_start_at")
    var recruitmentStartAt: LocalDateTime? = null /* 모집 시작 일시, 모집이 없는 콘텐츠는 null */
        protected set

    @Column(name = "recruitment_end_at")
    var recruitmentEndAt: LocalDateTime? = null /* 모집 종료 일시, 모집이 없는 콘텐츠는 null */
        protected set

    @Column(name = "content_hash", nullable = false, length = CONTENT_HASH_LENGTH)
    var contentHash: String = "" /* 태그에 쓰는 칸(종류·분류·제목·설명·단서)의 해시 */
        protected set

    @Column(name = "tag_job_fields", length = 1000)
    var tagJobFields: String? = null /* 추천 태그: 직군 enum 이름, 쉼표로 구분 */
        protected set

    @Column(name = "tag_job_roles", columnDefinition = "TEXT")
    var tagJobRoles: String? = null /* 추천 태그: 직무 enum 이름, 쉼표로 구분 */
        protected set

    @Column(name = "tag_topics", length = 1000)
    var tagTopics: String? = null /* 추천 태그: 준비 단계 enum 이름, 쉼표로 구분 */
        protected set

    @Column(name = "tagged_content_hash", length = CONTENT_HASH_LENGTH)
    var taggedContentHash: String? = null /* 태그를 붙일 때의 contentHash */
        protected set

    @Column(name = "tagged_at")
    var taggedAt: LocalDateTime? = null /* 태그를 붙인 일시 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 렛츠커리어 목록에서 빠진 일시 */
        protected set

    init {
        apply(source)
    }

    /** 렛츠커리어 목록의 지금 값으로 바꾼다. 지운 것으로 표시돼 있었으면 되살린다. */
    fun replace(source: LetsCareerContentSource) {
        apply(source)
        deletedAt = null
    }

    /** 렛츠커리어 목록에서 빠졌다. 반복해도 처음 빠진 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    /**
     * 추천 태그를 붙인다. `contentHash`는 태그할 대상을 받을 때의 해시이며 지금 해시와 같은지는 호출자가 확인한다.
     */
    fun tag(tags: LetsCareerContentTags, now: LocalDateTime) {
        tagJobFields = tags.jobFields.joinNames()
        tagJobRoles = tags.jobRoles.joinNames()
        tagTopics = tags.topics.joinNames()
        taggedContentHash = contentHash
        taggedAt = now
    }

    /** 지금 내용에 대한 태그다. 태그가 없거나 그 뒤로 제목·설명이 바뀌었으면 `null`이다. */
    fun currentTags(): LetsCareerContentTags? {
        if (taggedContentHash != contentHash) {
            return null
        }
        return LetsCareerContentTags(
            jobFields = parseNames(tagJobFields, JobField::valueOf),
            jobRoles = parseNames(tagJobRoles, JobRole::valueOf),
            topics = parseNames(tagTopics, LetsCareerContentTopic::valueOf),
        )
    }

    fun labelList(): List<String> = labels?.split(LABEL_SEPARATOR)?.filter(String::isNotBlank).orEmpty()

    private fun apply(source: LetsCareerContentSource) {
        require(source.title.isNotBlank()) { "렛츠커리어 콘텐츠 제목이 비어 있습니다." }
        require(source.path.startsWith("/")) { "렛츠커리어 콘텐츠 경로는 /로 시작해야 합니다." }
        category = source.category?.take(100)
        title = source.title.take(MAX_TITLE_LENGTH)
        description = source.description
        thumbnailUrl = source.thumbnailUrl?.takeIf { it.length <= MAX_URL_LENGTH }
        path = source.path.take(MAX_URL_LENGTH)
        labels = source.labels.map(String::trim).filter(String::isNotEmpty).joinToString(LABEL_SEPARATOR).ifEmpty { null }
        recruitmentStartAt = source.recruitmentStartAt
        recruitmentEndAt = source.recruitmentEndAt
        contentHash = hash(listOf(kind.name, category, title, description, labels))
    }

    companion object {
        const val MAX_TITLE_LENGTH = 500
        const val MAX_URL_LENGTH = 1000
        const val CONTENT_HASH_LENGTH = 64
        private const val LABEL_SEPARATOR = "\n"
        private const val NAME_SEPARATOR = ","
        private const val HASH_SEPARATOR = "\u0000"

        private fun hash(values: List<String?>): String =
            MessageDigest.getInstance("SHA-256")
                .digest(values.joinToString(HASH_SEPARATOR) { it.orEmpty() }.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        private fun Set<Enum<*>>.joinNames(): String? = map { it.name }.sorted().joinToString(NAME_SEPARATOR).ifEmpty { null }

        /** enum 이름이 바뀌어 읽을 수 없는 값은 버린다. 다음에 다시 태그하면 채워진다. */
        private fun <T> parseNames(names: String?, valueOf: (String) -> T): Set<T> =
            names?.split(NAME_SEPARATOR)
                ?.filter(String::isNotBlank)
                ?.mapNotNull { runCatching { valueOf(it) }.getOrNull() }
                ?.toSet()
                .orEmpty()
    }
}

/** 렛츠커리어 목록의 콘텐츠 하나에서 사본에 담는 값이다. */
data class LetsCareerContentSource(
    val category: String?,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val path: String,
    val labels: List<String>,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
)
