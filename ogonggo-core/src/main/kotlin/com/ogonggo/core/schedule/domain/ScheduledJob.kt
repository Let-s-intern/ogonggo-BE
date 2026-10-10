package com.ogonggo.core.schedule.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 스케줄 작업의 실행 주기와 켜짐 여부다. 운영자가 DB에서 직접 바꾼다.
 *
 * 행은 애플리케이션이 기동할 때 코드의 기본값으로 한 번만 만든다. 이미 있으면 건드리지 않으므로
 * DB에서 바꾼 값이 배포로 되돌아가지 않는다. 작업을 멈출 때는 행을 지우지 않고 `enabled`를 끈다.
 */
@Entity
@Table(
    name = "scheduled_jobs",
    uniqueConstraints = [UniqueConstraint(name = "uk_scheduled_job_name", columnNames = ["name"])],
)
internal class ScheduledJob(
    @Column(nullable = false, length = 64)
    val name: String, /* 작업 이름. ShedLock 잠금 이름과 같다 */

    @Column(nullable = false, length = 100)
    var cron: String, /* Spring cron 6자리(초 분 시 일 월 요일), Asia/Seoul 기준 */

    @Column(nullable = false)
    var enabled: Boolean, /* 꺼져 있으면 예약 시각이 와도 실행하지 않는다 */

    @Column(nullable = false, length = 200)
    var description: String, /* 작업 설명 */
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 스케줄 작업 식별자 */
        protected set
}
