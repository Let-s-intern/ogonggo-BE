package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.domain.JobMetric
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobSourceUrlClick
import com.ogonggo.core.job.domain.JobTag
import com.ogonggo.core.job.domain.Tag
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface JobJpaRepository : JpaRepository<Job, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Job?

    fun existsBySourceUrlAndDeletedAtIsNull(sourceUrl: String): Boolean

    fun findByIdAndPublicationStatusAndDeletedAtIsNull(
        id: Long,
        publicationStatus: JobPublicationStatus,
    ): Job?

    @Query(
        value = """
            select job
            from Job job
            join JobBookmark bookmark on bookmark.jobId = job.id
            where bookmark.userId = :userId
              and bookmark.deletedAt is null
              and job.publicationStatus = :publicationStatus
              and job.deletedAt is null
            order by bookmark.updatedAt desc, bookmark.id desc
        """,
        countQuery = """
            select count(job)
            from Job job
            join JobBookmark bookmark on bookmark.jobId = job.id
            where bookmark.userId = :userId
              and bookmark.deletedAt is null
              and job.publicationStatus = :publicationStatus
              and job.deletedAt is null
        """,
    )
    fun findBookmarkedJobs(
        @Param("userId") userId: Long,
        @Param("publicationStatus") publicationStatus: JobPublicationStatus,
        pageable: Pageable,
    ): Page<Job>

    @Query(
        """
        select job
        from Job job
        where job.publicationStatus = :publicationStatus
          and job.deletedAt is null
          and job.recruitmentStartAt is not null
          and job.recruitmentEndAt is not null
          and job.recruitmentStartAt < :rangeEndExclusive
          and job.recruitmentEndAt >= :rangeStart
        order by job.recruitmentEndAt asc, job.id asc
        """,
    )
    fun findPublishedCalendarJobs(
        @Param("publicationStatus") publicationStatus: JobPublicationStatus,
        @Param("rangeStart") rangeStart: LocalDateTime,
        @Param("rangeEndExclusive") rangeEndExclusive: LocalDateTime,
    ): List<Job>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from Job job where job.id = :jobId and job.deletedAt is null")
    fun findByIdForUpdate(@Param("jobId") jobId: Long): Job?

    /** 북마크 해제는 이미 삭제된 공고에도 허용하므로 삭제 여부를 가리지 않고 조회한다. */
    @Query("select job from Job job where job.id = :jobId")
    fun findIncludingDeletedById(@Param("jobId") jobId: Long): Job?

    fun findByIdAndOwnerUserIdAndDeletedAtIsNull(id: Long, ownerUserId: Long): Job?

    fun findAllByOwnerUserIdAndDeletedAtIsNull(ownerUserId: Long, pageable: Pageable): Page<Job>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select job
        from Job job
        where job.id = :jobId
          and job.ownerUserId = :ownerUserId
          and job.deletedAt is null
        """,
    )
    fun findOwnedByIdForUpdate(
        @Param("ownerUserId") ownerUserId: Long,
        @Param("jobId") jobId: Long,
    ): Job?

    /** 삭제는 멱등해야 하므로 이미 삭제된 공고도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from Job job where job.id = :jobId and job.ownerUserId = :ownerUserId")
    fun findOwnedByIdForDelete(
        @Param("ownerUserId") ownerUserId: Long,
        @Param("jobId") jobId: Long,
    ): Job?
}

internal interface JobMetricJpaRepository : JpaRepository<JobMetric, Long> {
    fun findByJobId(jobId: Long): JobMetric?

    fun findAllByJobIdIn(jobIds: Collection<Long>): List<JobMetric>

    /** 동시 조회에서도 증가분이 유실되지 않도록 읽고 쓰지 않고 한 번의 UPDATE로 증가시킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update JobMetric metric
        set metric.viewCount = metric.viewCount + 1,
            metric.updatedAt = :now
        where metric.jobId = :jobId
        """,
    )
    fun increaseViewCount(@Param("jobId") jobId: Long, @Param("now") now: LocalDateTime): Int

    /**
     * 활성 북마크를 다시 세어 맞춘다.
     * 세는 일을 UPDATE 안에서 처리해 읽고 쓰는 사이에 다른 갱신이 끼어들지 못하게 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update JobMetric metric
        set metric.bookmarkCount = (
                select count(bookmark)
                from JobBookmark bookmark
                where bookmark.jobId = :jobId
                  and bookmark.deletedAt is null
            ),
            metric.updatedAt = :now
        where metric.jobId = :jobId
        """,
    )
    fun syncBookmarkCount(@Param("jobId") jobId: Long, @Param("now") now: LocalDateTime): Int
}

internal interface JobBookmarkJpaRepository : JpaRepository<JobBookmark, Long> {
    fun findByJobIdAndUserId(jobId: Long, userId: Long): JobBookmark?

    /**
     * 해제된 북마크를 다시 활성으로 되돌린다.
     * 조회한 값으로 분기하지 않고 조건을 UPDATE에 넣어, 동시에 들어온 해제 요청과 순서가 뒤집히지 않게 한다.
     * 벌크 연산은 Auditing을 거치지 않으므로 북마크 목록의 정렬 기준인 수정 일시를 함께 기록한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update JobBookmark bookmark
        set bookmark.deletedAt = null,
            bookmark.updatedAt = :now
        where bookmark.jobId = :jobId
          and bookmark.userId = :userId
          and bookmark.deletedAt is not null
        """,
    )
    fun restore(
        @Param("jobId") jobId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    /** 활성 북마크만 해제한다. 이미 해제된 북마크는 갱신 대상이 아니므로 최초 해제 일시가 덮어써지지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update JobBookmark bookmark
        set bookmark.deletedAt = :now,
            bookmark.updatedAt = :now
        where bookmark.jobId = :jobId
          and bookmark.userId = :userId
          and bookmark.deletedAt is null
        """,
    )
    fun softDelete(
        @Param("jobId") jobId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select bookmark.jobId
        from JobBookmark bookmark
        where bookmark.userId = :userId
          and bookmark.jobId in :jobIds
          and bookmark.deletedAt is null
        """,
    )
    fun findActiveJobIds(
        @Param("userId") userId: Long,
        @Param("jobIds") jobIds: Collection<Long>,
    ): Set<Long>
}

internal interface JobSourceUrlClickJpaRepository : JpaRepository<JobSourceUrlClick, Long> {
    fun existsByJobIdAndUserId(jobId: Long, userId: Long): Boolean
}

internal interface TagJpaRepository : JpaRepository<Tag, Long> {
    fun findAllByNameIn(names: Collection<String>): List<Tag>
}

internal interface JobTagJpaRepository : JpaRepository<JobTag, Long> {
    fun findAllByJobId(jobId: Long): List<JobTag>
}
