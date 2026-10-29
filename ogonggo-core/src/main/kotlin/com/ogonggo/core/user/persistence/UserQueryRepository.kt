package com.ogonggo.core.user.persistence

import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.QCompanyProfile.companyProfile
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.QUserProfile.userProfile
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.domain.UserRole
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 관리자 콘솔 회원 목록이다. 프로필 행이 없는 회원도 목록에 나와야 하므로 프로필은 left join 한다.
 * 가입 순서와 식별자 순서가 같으므로 최근 가입 순은 `id DESC`다.
 */
@Repository
internal class UserQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findGeneralMemberPage(
        condition: UserManagementSearchCondition,
        pageable: Pageable,
    ): Page<Pair<User, UserProfile?>> {
        val predicates = arrayOf(
            user.role.`in`(UserRole.GENERAL_MEMBER_ROLES),
            condition.role?.let(user.role::eq),
            condition.inflowSource?.let(user.inflowSource::eq),
            *commonPredicates(condition),
            condition.keyword?.takeIf { it.isNotBlank() }?.let {
                userProfile.name.containsIgnoreCase(it)
                    .or(userProfile.nickname.containsIgnoreCase(it))
                    .or(userProfile.email.containsIgnoreCase(it))
            },
        )

        val content = queryFactory.select(user, userProfile)
            .from(user)
            .leftJoin(userProfile).on(userProfile.userId.eq(user.id))
            .where(*predicates)
            .orderBy(user.id.desc())
            .paged(pageable)
            .fetch()
            .map { row -> checkNotNull(row.get(user)) to row.get(userProfile) }

        val countQuery = queryFactory.select(user.count())
            .from(user)
            .leftJoin(userProfile).on(userProfile.userId.eq(user.id))
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }

    fun findCompanyMemberPage(
        condition: UserManagementSearchCondition,
        pageable: Pageable,
    ): Page<Pair<User, CompanyProfile?>> {
        val predicates = arrayOf(
            user.role.eq(UserRole.COMPANY),
            *commonPredicates(condition),
            condition.keyword?.takeIf { it.isNotBlank() }?.let {
                companyProfile.organizationName.containsIgnoreCase(it)
                    .or(companyProfile.managerName.containsIgnoreCase(it))
                    .or(user.email.containsIgnoreCase(it))
            },
        )

        val content = queryFactory.select(user, companyProfile)
            .from(user)
            .leftJoin(companyProfile).on(companyProfile.userId.eq(user.id))
            .where(*predicates)
            .orderBy(user.id.desc())
            .paged(pageable)
            .fetch()
            .map { row -> checkNotNull(row.get(user)) to row.get(companyProfile) }

        val countQuery = queryFactory.select(user.count())
            .from(user)
            .leftJoin(companyProfile).on(companyProfile.userId.eq(user.id))
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }

    /** 관리자 콘솔의 일반 회원 상세다. 기업 회원이면 일반 회원이 아니므로 null이다. */
    fun findGeneralMember(userId: Long): Pair<User, UserProfile?>? =
        queryFactory.select(user, userProfile)
            .from(user)
            .leftJoin(userProfile).on(userProfile.userId.eq(user.id))
            .where(user.id.eq(userId), user.role.`in`(UserRole.GENERAL_MEMBER_ROLES))
            .fetchOne()
            ?.let { row -> checkNotNull(row.get(user)) to row.get(userProfile) }

    /** 관리자 콘솔의 기업 회원 상세다. 일반 회원이면 null이다. */
    fun findCompanyMember(userId: Long): Pair<User, CompanyProfile?>? =
        queryFactory.select(user, companyProfile)
            .from(user)
            .leftJoin(companyProfile).on(companyProfile.userId.eq(user.id))
            .where(user.id.eq(userId), user.role.eq(UserRole.COMPANY))
            .fetchOne()
            ?.let { row -> checkNotNull(row.get(user)) to row.get(companyProfile) }

    private fun commonPredicates(condition: UserManagementSearchCondition): Array<Predicate?> = arrayOf(
        condition.status?.let(user.status::eq),
        condition.joinedFrom?.let { user.joinedAt.goe(it.atStartOfDay()) },
        condition.joinedTo?.let { user.joinedAt.lt(it.plusDays(1).atStartOfDay()) },
    )
}
