package com.ogonggo.core.bootcamp.persistence

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampBookmark
import com.ogonggo.core.bootcamp.domain.BootcampCurriculum
import com.ogonggo.core.bootcamp.domain.BootcampMetric
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.bootcamp.domain.BootcampStatus
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

    @Query(
        """
        select bootcamp
        from Bootcamp bootcamp
        where bootcamp.id = :bootcampId
          and bootcamp.status in :statuses
          and bootcamp.deletedAt is null
          and (bootcamp.publicationStartAt is null or bootcamp.publicationStartAt <= :now)
          and (bootcamp.publicationEndAt is null or bootcamp.publicationEndAt >= :now)
        """,
    )
    fun findPublicById(
        @Param("bootcampId") bootcampId: Long,
        @Param("statuses") statuses: Collection<BootcampStatus>,
        @Param("now") now: LocalDateTime,
    ): Bootcamp?

    @Query(
        """
        select bootcamp
        from Bootcamp bootcamp
        where bootcamp.status in :statuses
          and bootcamp.deletedAt is null
          and (bootcamp.publicationStartAt is null or bootcamp.publicationStartAt <= :now)
          and (bootcamp.publicationEndAt is null or bootcamp.publicationEndAt >= :now)
        """,
    )
    fun findAllPublic(
        @Param("statuses") statuses: Collection<BootcampStatus>,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): Page<Bootcamp>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bootcamp from Bootcamp bootcamp where bootcamp.id = :bootcampId and bootcamp.deletedAt is null")
    fun findByIdForUpdate(@Param("bootcampId") bootcampId: Long): Bootcamp?

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

    /**
     * 조회 수는 지표 테이블이 소유하고 부트캠프와 연관관계가 없으므로 명시적으로 조인한다.
     * 지표 행은 첫 조회 시점에 생기므로 아직 없는 부트캠프는 0으로 본다.
     * 조회 수가 같을 때 페이지가 흔들리지 않도록 식별자로 순서를 확정한다.
     */
    @Query(
        value = """
        select bootcamp
        from Bootcamp bootcamp
        left join BootcampMetric metric on metric.bootcampId = bootcamp.id
        where bootcamp.status in :statuses
          and bootcamp.deletedAt is null
          and (bootcamp.publicationStartAt is null or bootcamp.publicationStartAt <= :now)
          and (bootcamp.publicationEndAt is null or bootcamp.publicationEndAt >= :now)
        order by coalesce(metric.viewCount, 0) desc, bootcamp.id desc
        """,
        countQuery = """
        select count(bootcamp)
        from Bootcamp bootcamp
        where bootcamp.status in :statuses
          and bootcamp.deletedAt is null
          and (bootcamp.publicationStartAt is null or bootcamp.publicationStartAt <= :now)
          and (bootcamp.publicationEndAt is null or bootcamp.publicationEndAt >= :now)
        """,
    )
    fun findAllPublicOrderByViewCount(
        @Param("statuses") statuses: Collection<BootcampStatus>,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): Page<Bootcamp>
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
}

internal interface BootcampBookmarkJpaRepository : JpaRepository<BootcampBookmark, Long> {
    fun findByBootcampIdAndUserId(bootcampId: Long, userId: Long): BootcampBookmark?
    fun countByBootcampIdAndDeletedAtIsNull(bootcampId: Long): Long
}

internal interface BootcampPartnerJpaRepository : JpaRepository<BootcampPartner, Long> {
    fun findAllByBootcampId(bootcampId: Long): List<BootcampPartner>
    fun findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampPartner>
    fun findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampPartner>
}

internal interface BootcampCurriculumJpaRepository : JpaRepository<BootcampCurriculum, Long> {
    fun findAllByBootcampIdAndDeletedAtIsNullOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampCurriculum>
    fun findAllByBootcampIdOrderByDisplayOrderAsc(bootcampId: Long): List<BootcampCurriculum>
}
