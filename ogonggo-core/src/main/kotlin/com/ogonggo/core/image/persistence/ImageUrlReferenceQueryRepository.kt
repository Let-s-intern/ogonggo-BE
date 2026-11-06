package com.ogonggo.core.image.persistence

import com.ogonggo.core.banner.domain.QBanner.banner
import com.ogonggo.core.bootcamp.domain.QBootcamp.bootcamp
import com.ogonggo.core.job.domain.QJob.job
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * 이미지를 식별자가 아니라 URL로 저장하는 칼럼(채용공고 `logo_url`·`cover_image_url`, 부트캠프 `logo_url`·`representative_image_url`)이
 * 이미지 URL을 쓰는지 본다. 이 칼럼들은 이미지 상태를 맞추지 않고 저장될 수 있어(관리자 수정, AI 등록, 기업 로고 재사용),
 * 참조를 풀거나 지우기 전에 실제 사용 여부를 확인한다. 삭제된 공고도 되살릴 수 있어 함께 본다.
 *
 * 배너(`pc_image_url`·`mobile_image_url`)도 이미지를 URL로만 저장하고 첨부하지 않으므로 함께 본다. 지운 배너도 본다.
 */
@Repository
internal class ImageUrlReferenceQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** [urls] 중 채용공고·부트캠프의 로고·대표 이미지, 배너 이미지로 쓰이는 URL을 돌려준다. */
    fun findUrlsUsedByPostings(urls: Collection<String>): Set<String> {
        if (urls.isEmpty()) return emptySet()
        val jobUrls = queryFactory
            .select(job.logoUrl, job.coverImageUrl)
            .distinct()
            .from(job)
            .where(job.logoUrl.`in`(urls).or(job.coverImageUrl.`in`(urls)))
            .fetch()
            .flatMap { listOf(it.get(job.logoUrl), it.get(job.coverImageUrl)) }
        val bootcampUrls = queryFactory
            .select(bootcamp.logoUrl, bootcamp.representativeImageUrl)
            .distinct()
            .from(bootcamp)
            .where(bootcamp.logoUrl.`in`(urls).or(bootcamp.representativeImageUrl.`in`(urls)))
            .fetch()
            .flatMap { listOf(it.get(bootcamp.logoUrl), it.get(bootcamp.representativeImageUrl)) }
        val bannerUrls = queryFactory
            .select(banner.pcImageUrl, banner.mobileImageUrl)
            .distinct()
            .from(banner)
            .where(banner.pcImageUrl.`in`(urls).or(banner.mobileImageUrl.`in`(urls)))
            .fetch()
            .flatMap { listOf(it.get(banner.pcImageUrl), it.get(banner.mobileImageUrl)) }
        return (jobUrls + bootcampUrls + bannerUrls).filterNotNull().filter { it in urls }.toSet()
    }
}
