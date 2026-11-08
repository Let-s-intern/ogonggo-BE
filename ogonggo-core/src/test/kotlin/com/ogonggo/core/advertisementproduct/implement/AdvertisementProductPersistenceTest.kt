package com.ogonggo.core.advertisementproduct.implement

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertisementProductAppender::class,
    AdvertisementProductManager::class,
    AdvertisementProductReader::class,
    AdvertisementProductQueryRepository::class,
)
internal class AdvertisementProductPersistenceTest @Autowired constructor(
    private val appender: AdvertisementProductAppender,
    private val manager: AdvertisementProductManager,
    private val reader: AdvertisementProductReader,
    private val entityManager: EntityManager,
) {

    @Test
    fun `상품 목록은 카테고리 선언 순서와 상품 ID 오름차순으로 주고 삭제 상품은 제외한다`() {
        // given
        val bannerFirst = append(AdvertisementProductCategory.BANNER, "배너 첫 상품")
        val instagramFirst = append(AdvertisementProductCategory.INSTAGRAM_1, "인스타그램 첫 상품")
        val postingFirst = append(AdvertisementProductCategory.JOB_POSTING, "공고 첫 상품")
        val deletedBanner = append(AdvertisementProductCategory.BANNER, "삭제할 배너")
        val kakaoLowerId = append(AdvertisementProductCategory.KAKAO_1, "카카오 낮은 ID 상품")
        val bannerSecond = append(AdvertisementProductCategory.BANNER, "배너 두 번째 상품")
        val postingSecond = append(AdvertisementProductCategory.JOB_POSTING, "공고 두 번째 상품")
        val kakaoHigherId = append(AdvertisementProductCategory.KAKAO_1, "카카오 높은 ID 상품")
        val instagramSecond = append(AdvertisementProductCategory.INSTAGRAM_1, "인스타그램 두 번째 상품")
        val instagramOtherChannel = append(AdvertisementProductCategory.INSTAGRAM_2, "다른 인스타그램 상품")
        val otherKakaoChannel = append(AdvertisementProductCategory.KAKAO_2, "다른 카카오 상품")
        manager.delete(reader.read(deletedBanner), NOW)
        entityManager.flush()
        entityManager.clear()

        // when
        val products = reader.readAll()
        val firstPage = reader.readPage(page = 0, size = 3)
        val secondPage = reader.readPage(page = 1, size = 3)

        // then
        assertEquals(
            listOf(
                AdvertisementProductCategory.JOB_POSTING to postingFirst,
                AdvertisementProductCategory.JOB_POSTING to postingSecond,
                AdvertisementProductCategory.BANNER to bannerFirst,
                AdvertisementProductCategory.BANNER to bannerSecond,
                AdvertisementProductCategory.INSTAGRAM_1 to instagramFirst,
                AdvertisementProductCategory.INSTAGRAM_1 to instagramSecond,
                AdvertisementProductCategory.INSTAGRAM_2 to instagramOtherChannel,
                AdvertisementProductCategory.KAKAO_1 to kakaoLowerId,
                AdvertisementProductCategory.KAKAO_1 to kakaoHigherId,
                AdvertisementProductCategory.KAKAO_2 to otherKakaoChannel,
            ),
            products.map { it.category to checkNotNull(it.id) },
        )
        assertEquals(listOf(postingFirst, postingSecond, bannerFirst), firstPage.items.map { it.id })
        assertEquals(listOf(bannerSecond, instagramFirst, instagramSecond), secondPage.items.map { it.id })
        assertEquals(10L, firstPage.totalElements)
        assertEquals(4, firstPage.totalPages)
        assertEquals(1, secondPage.page)
        assertEquals(3, secondPage.size)
    }

    private fun append(category: AdvertisementProductCategory, name: String): Long = appender.append(
        AdvertisementProductSaveDto(
            category = category,
            position = if (category == AdvertisementProductCategory.BANNER) {
                AdvertisementProductPosition.JOB_HOME_TOP_BANNER
            } else {
                null
            },
            name = name,
            price = null,
        ),
    )

    companion object {
        private val NOW = LocalDateTime.of(2026, 11, 8, 12, 0)
    }
}
