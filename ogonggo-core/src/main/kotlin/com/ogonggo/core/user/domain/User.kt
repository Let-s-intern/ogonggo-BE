package com.ogonggo.core.user.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 오공고 계정은 두 종류다.
 * 일반 회원은 렛츠커리어 계정과 1:1 대응하고 오공고가 자격증명을 갖지 않는다.
 * 기업 회원은 렛츠커리어를 거치지 않고 오공고가 이메일과 비밀번호를 직접 소유한다.
 * 두 계정은 서로 무관하며 같은 사람이 양쪽을 따로 가질 수 있다.
 * 잘못된 조합이 만들어지지 않도록 생성자를 감추고 종류별 팩토리만 노출한다.
 */
@Entity
@Table(name = "users")
internal class User private constructor(
    @Column(name = "letscareer_user_id", unique = true)
    val letsCareerUserId: Long?, /* 렛츠커리어 사용자 식별자. 기업 회원은 없다 */

    @Column(name = "email", unique = true, length = 255)
    val email: String?, /* 로그인 이메일. 일반 회원은 없다 */

    password: String?,

    role: UserRole,

    status: UserStatus,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: LocalDateTime, /* 오공고 가입 일시 */
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 사용자 식별자 */
        protected set

    @Column(name = "password", length = 100)
    var password: String? = password /* 인코딩된 비밀번호. 일반 회원은 없다 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserStatus = status /* 사용자 상태 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = role /* 오공고 사용자 역할 */
        protected set

    @Column(name = "withdrawn_at")
    var withdrawnAt: LocalDateTime? = null /* 탈퇴 일시 */
        protected set

    fun withdraw(now: LocalDateTime) {
        if (status == UserStatus.WITHDRAWN) {
            return
        }

        status = UserStatus.WITHDRAWN
        withdrawnAt = now
    }

    fun suspend() {
        check(status != UserStatus.WITHDRAWN) { "탈퇴한 사용자는 정지할 수 없습니다." }
        status = UserStatus.SUSPENDED
    }

    fun activate() {
        check(status != UserStatus.WITHDRAWN) { "탈퇴한 사용자는 활성화할 수 없습니다." }
        status = UserStatus.ACTIVE
    }

    companion object {
        /** 렛츠커리어 토큰 교환으로 만들어지는 일반 회원 계정. */
        fun ofLetsCareer(letsCareerUserId: Long, joinedAt: LocalDateTime): User {
            require(letsCareerUserId > 0) { "렛츠커리어 사용자 식별자는 양수여야 합니다." }

            return User(
                letsCareerUserId = letsCareerUserId,
                email = null,
                password = null,
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                joinedAt = joinedAt,
            )
        }

        /**
         * 오공고 기업용 회원가입으로 만들어지는 계정.
         * 승인 절차가 없으므로 생성 시점에 바로 COMPANY 역할과 활성 상태를 갖는다.
         * 비밀번호는 이미 인코딩된 값만 받는다. 인코딩 방식은 API 모듈이 소유한다.
         */
        fun ofCompany(email: String, encodedPassword: String, joinedAt: LocalDateTime): User {
            require(email.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
            require(encodedPassword.isNotBlank()) { "비밀번호는 비어 있을 수 없습니다." }

            return User(
                letsCareerUserId = null,
                email = email,
                password = encodedPassword,
                role = UserRole.COMPANY,
                status = UserStatus.ACTIVE,
                joinedAt = joinedAt,
            )
        }
    }
}
