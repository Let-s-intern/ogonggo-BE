package com.ogonggo.core.bootcamp.persistence

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampApplicationUrlClick
import com.ogonggo.core.bootcamp.domain.BootcampBookmark
import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.review.domain.ReviewStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface BootcampJpaRepository : JpaRepository<Bootcamp, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Bootcamp?

    fun findByIdAndOwnerUserIdAndDeletedAtIsNull(id: Long, ownerUserId: Long): Bootcamp?

    fun existsBySourceUrlAndDeletedAtIsNull(sourceUrl: String): Boolean

    /** 원문 URL은 등록 시점에만 중복을 막고 DB 제약이 없으므로, 겹친 행이 있어도 가장 먼저 등록된 행을 고른다. */
    fun findFirstBySourceUrlAndOwnerUserIdIsNullAndDeletedAtIsNullOrderByIdAsc(sourceUrl: String): Bootcamp?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select bootcamp from Bootcamp bootcamp " +
            "where bootcamp.id = :bootcampId and bootcamp.ownerUserId is null " +
            "and bootcamp.sourceUrl is not null and bootcamp.deletedAt is null",
    )
    fun findCrawledByIdForUpdate(@Param("bootcampId") bootcampId: Long): Bootcamp?

    /** 크롤러 삭제는 멱등해야 하므로 이미 삭제된 수집 부트캠프도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select bootcamp from Bootcamp bootcamp " +
            "where bootcamp.id = :bootcampId and bootcamp.ownerUserId is null and bootcamp.sourceUrl is not null",
    )
    fun findCrawledByIdForDelete(@Param("bootcampId") bootcampId: Long): Bootcamp?

    @Query(
        """
        select bootcamp
        from Bootcamp bootcamp
        where bootcamp.id = :bootcampId
          and bootcamp.publicationStatus = :publicationStatus
          and bootcamp.status in :statuses
          and bootcamp.deletedAt is null
          and (bootcamp.publicationStartAt is null or bootcamp.publicationStartAt <= :now)
          and (bootcamp.publicationEndAt is null or bootcamp.publicationEndAt >= :now)
        """,
    )
    fun findPublicById(
        @Param("bootcampId") bootcampId: Long,
        @Param("statuses") statuses: Collection<BootcampStatus>,
        @Param("publicationStatus") publicationStatus: BootcampPublicationStatus,
        @Param("now") now: LocalDateTime,
    ): Bootcamp?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bootcamp from Bootcamp bootcamp where bootcamp.id = :bootcampId and bootcamp.deletedAt is null")
    fun findByIdForUpdate(@Param("bootcampId") bootcampId: Long): Bootcamp?

    /** 북마크 해제는 이미 삭제된 부트캠프에도 허용하므로 삭제 여부를 가리지 않고 조회한다. */
    @Query("select bootcamp from Bootcamp bootcamp where bootcamp.id = :bootcampId")
    fun findIncludingDeletedById(@Param("bootcampId") bootcampId: Long): Bootcamp?

    /** 관리자 삭제는 멱등해야 하므로 이미 삭제된 부트캠프도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bootcamp from Bootcamp bootcamp where bootcamp.id = :bootcampId")
    fun findIncludingDeletedByIdForUpdate(@Param("bootcampId") bootcampId: Long): Bootcamp?

    fun findAllByReviewStatusAndDeletedAtIsNullOrderByIdAsc(reviewStatus: ReviewStatus): List<Bootcamp>

    fun countByReviewStatusAndDeletedAtIsNull(reviewStatus: ReviewStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select bootcamp from Bootcamp bootcamp " +
            "where bootcamp.id = :bootcampId and bootcamp.ownerUserId = :ownerUserId " +
            "and bootcamp.deletedAt is null",
    )
    fun findOwnedByIdForUpdate(
        @Param("ownerUserId") ownerUserId: Long,
        @Param("bootcampId") bootcampId: Long,
    ): Bootcamp?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select bootcamp from Bootcamp bootcamp " +
            "where bootcamp.id = :bootcampId and bootcamp.ownerUserId = :ownerUserId",
    )
    fun findOwnedByIdForDelete(
        @Param("ownerUserId") ownerUserId: Long,
        @Param("bootcampId") bootcampId: Long,
    ): Bootcamp?

    fun findAllByOwnerUserIdAndDeletedAtIsNull(ownerUserId: Long, pageable: Pageable): Page<Bootcamp>
}

internal interface BootcampMetricJpaRepository : JpaRepository<BootcampMetric, Long> {
    fun findByBootcampId(bootcampId: Long): BootcampMetric?

    fun findAllByBootcampIdIn(bootcampIds: Collection<Long>): List<BootcampMetric>

    /** 동시 조회에서도 증가분이 유실되지 않도록 읽고 쓰지 않고 한 번의 UPDATE로 증가시킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update BootcampMetric metric
        set metric.viewCount = metric.viewCount + 1,
            metric.updatedAt = :now
        where metric.bootcampId = :bootcampId
        """,
    )
    fun increaseViewCount(
        @Param("bootcampId") bootcampId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    /**
     * 활성 북마크를 다시 세어 맞춘다.
     * 세는 일을 UPDATE 안에서 처리해 읽고 쓰는 사이에 다른 갱신이 끼어들지 못하게 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update BootcampMetric metric
        set metric.bookmarkCount = (
                select count(bookmark)
                from BootcampBookmark bookmark
                where bookmark.bootcampId = :bootcampId
                  and bookmark.deletedAt is null
            ),
            metric.updatedAt = :now
        where metric.bootcampId = :bootcampId
        """,
    )
    fun syncBookmarkCount(
        @Param("bootcampId") bootcampId: Long,
        @Param("now") now: LocalDateTime,
    ): Int
}

internal interface BootcampBookmarkJpaRepository : JpaRepository<BootcampBookmark, Long> {
    fun findByBootcampIdAndUserId(bootcampId: Long, userId: Long): BootcampBookmark?

    /**
     * 해제된 북마크를 다시 활성으로 되돌리고 지원·신청 관리 단계는 스크랩부터 다시 시작한다.
     * 조회한 값으로 분기하지 않고 조건을 UPDATE에 넣어, 동시에 들어온 해제 요청과 순서가 뒤집히지 않게 한다.
     * 벌크 연산은 Auditing을 거치지 않으므로 북마크 목록의 정렬 기준인 수정 일시를 함께 기록한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update BootcampBookmark bookmark
        set bookmark.deletedAt = null,
            bookmark.applicationStatus = com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus.SCRAPPED,
            bookmark.updatedAt = :now
        where bookmark.bootcampId = :bootcampId
          and bookmark.userId = :userId
          and bookmark.deletedAt is not null
        """,
    )
    fun restore(
        @Param("bootcampId") bootcampId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    /**
     * 활성 북마크의 지원·신청 관리 단계를 옮긴다.
     * 조회한 단계로 분기하지 않고 출발 단계를 UPDATE 조건에 넣어, 동시에 들어온 다른 이동과 순서가 뒤집히지 않게 한다.
     * 벌크 연산은 Auditing을 거치지 않으므로 북마크 목록의 정렬 기준인 수정 일시를 함께 기록한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update BootcampBookmark bookmark
        set bookmark.applicationStatus = :target,
            bookmark.updatedAt = :now
        where bookmark.bootcampId = :bootcampId
          and bookmark.userId = :userId
          and bookmark.deletedAt is null
          and bookmark.applicationStatus in :sources
        """,
    )
    fun changeApplicationStatus(
        @Param("bootcampId") bootcampId: Long,
        @Param("userId") userId: Long,
        @Param("sources") sources: Collection<BootcampApplicationStatus>,
        @Param("target") target: BootcampApplicationStatus,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select bookmark.applicationStatus
        from BootcampBookmark bookmark
        where bookmark.bootcampId = :bootcampId
          and bookmark.userId = :userId
          and bookmark.deletedAt is null
        """,
    )
    fun findActiveApplicationStatus(
        @Param("bootcampId") bootcampId: Long,
        @Param("userId") userId: Long,
    ): BootcampApplicationStatus?

    /** 활성 북마크만 해제한다. 이미 해제된 북마크는 갱신 대상이 아니므로 최초 해제 일시가 덮어써지지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update BootcampBookmark bookmark
        set bookmark.deletedAt = :now,
            bookmark.updatedAt = :now
        where bookmark.bootcampId = :bootcampId
          and bookmark.userId = :userId
          and bookmark.deletedAt is null
        """,
    )
    fun softDelete(
        @Param("bootcampId") bootcampId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select bookmark.bootcampId
        from BootcampBookmark bookmark
        where bookmark.userId = :userId
          and bookmark.bootcampId in :bootcampIds
          and bookmark.deletedAt is null
        """,
    )
    fun findActiveBootcampIds(
        @Param("userId") userId: Long,
        @Param("bootcampIds") bootcampIds: Collection<Long>,
    ): Set<Long>
}

internal interface BootcampApplicationUrlClickJpaRepository :
    JpaRepository<BootcampApplicationUrlClick, Long> {
    fun existsByBootcampIdAndUserId(bootcampId: Long, userId: Long): Boolean
}

internal interface BootcampPartnerJpaRepository : JpaRepository<BootcampPartner, Long> {
    fun findAllByBootcampId(bootcampId: Long): List<BootcampPartner>
    fun findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampPartner>
    fun findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampPartner>
}

internal interface BootcampCurriculumJpaRepository : JpaRepository<BootcampCurriculum, Long> {
    fun findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampCurriculum>
    fun findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampCurriculum>
    fun findAllByBootcampIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(
        bootcampIds: Collection<Long>,
    ): List<BootcampCurriculum>
}
