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

    fun findAllByPublicationStatusAndDeletedAtIsNull(
        publicationStatus: JobPublicationStatus,
        pageable: Pageable,
    ): Page<Job>

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select job
        from Job job
        where job.id = :jobId
          and job.publicationStatus = :publicationStatus
          and job.deletedAt is null
        """,
    )
    fun findPublishedByIdForUpdate(
        @Param("jobId") jobId: Long,
        @Param("publicationStatus") publicationStatus: JobPublicationStatus,
    ): Job?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from Job job where job.id = :jobId")
    fun findIncludingDeletedByIdForUpdate(@Param("jobId") jobId: Long): Job?

    /**
     * 조회 수는 지표 테이블이 소유하고 공고와 연관관계가 없으므로 명시적으로 조인한다.
     * 지표 행은 첫 조회 시점에 생기므로 아직 없는 공고는 0으로 본다.
     * 조회 수가 같을 때 페이지가 흔들리지 않도록 식별자로 순서를 확정한다.
     */
    @Query(
        value = """
        select job
        from Job job
        left join JobMetric metric on metric.jobId = job.id
        where job.publicationStatus = :publicationStatus
          and job.deletedAt is null
        order by coalesce(metric.viewCount, 0) desc, job.id desc
        """,
        countQuery = """
        select count(job)
        from Job job
        where job.publicationStatus = :publicationStatus
          and job.deletedAt is null
        """,
    )
    fun findAllPublishedOrderByViewCount(
        @Param("publicationStatus") publicationStatus: JobPublicationStatus,
        pageable: Pageable,
    ): Page<Job>
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
}

internal interface JobBookmarkJpaRepository : JpaRepository<JobBookmark, Long> {
    fun findByJobIdAndUserId(jobId: Long, userId: Long): JobBookmark?
    fun countByJobIdAndDeletedAtIsNull(jobId: Long): Long

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
