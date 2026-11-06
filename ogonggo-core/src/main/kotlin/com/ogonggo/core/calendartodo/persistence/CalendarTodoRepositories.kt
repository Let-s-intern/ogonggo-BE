package com.ogonggo.core.calendartodo.persistence

import com.ogonggo.core.calendartodo.domain.CalendarTodo
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

internal interface CalendarTodoJpaRepository : JpaRepository<CalendarTodo, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CalendarTodo?

    /** 날짜(시작·끝 포함)가 `[from, toExclusive)`와 겹치는 할 일이다. 시작일, 식별자 순이다. */
    @Query(
        """
        select t from CalendarTodo t
        where t.deletedAt is null and t.startDate < :toExclusive and t.endDate >= :from
        order by t.startDate asc, t.id asc
        """,
    )
    fun findAllOverlapping(@Param("from") from: LocalDate, @Param("toExclusive") toExclusive: LocalDate): List<CalendarTodo>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CalendarTodo t where t.id = :todoId and t.deletedAt is null")
    fun findForUpdate(@Param("todoId") todoId: Long): CalendarTodo?

    /** 삭제는 멱등해야 하므로 이미 삭제된 할 일도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CalendarTodo t where t.id = :todoId")
    fun findIncludingDeletedForUpdate(@Param("todoId") todoId: Long): CalendarTodo?
}
