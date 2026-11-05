package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.image.persistence.ImageUrlReferenceQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import jakarta.persistence.EntityManager
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ImageUrlReferenceQueryRepository::class)
internal class ImageAssetPersistenceTest @Autowired constructor(
    private val repository: ImageAssetJpaRepository,
    private val references: ImageUrlReferenceQueryRepository,
    private val entityManager: EntityManager,
) {
    private val storage = Mockito.mock(S3ObjectClient::class.java)
    private val manager = ImageAssetManager(repository, storage, ObjectMapper(), references)

    @Test
    fun `공고에 저장해 첨부한 이미지는 보존 기간이 지나도 남고 저장하지 않은 업로드만 지운다`() {
        // given
        val saved = upload("saved-logo", OWNER_ID)
        val notSaved = upload("not-saved-logo", OWNER_ID)
        manager.attachImageUrls(OWNER_ID, listOf(saved.url, "https://example.com/external.png"))
        repository.flush()

        // when
        val deletedCount = manager.cleanup(LocalDateTime.now().plusDays(2), Duration.ofHours(1))

        // then
        assertEquals(1, deletedCount)
        assertEquals(ImageAssetStatus.ATTACHED, repository.findById(saved.id).orElseThrow().status)
        assertEquals(ImageAssetStatus.DELETED, repository.findById(notSaved.id).orElseThrow().status)
        Mockito.verify(storage).delete(notSaved.storageKey)
        Mockito.verify(storage, Mockito.never()).delete(saved.storageKey)
    }

    @Test
    fun `남의 이미지 URL을 공고에 저장해도 그 이미지는 첨부하지 않는다`() {
        // given
        val others = upload("others-logo", OWNER_ID + 1)

        // when
        manager.attachImageUrls(OWNER_ID, listOf(others.url))
        repository.flush()

        // then
        assertEquals(ImageAssetStatus.TEMPORARY, repository.findById(others.id).orElseThrow().status)
    }

    @Test
    fun `기업 로고를 바꿔도 공고와 부트캠프가 URL로 쓰는 이전 로고는 정리 작업이 지우지 않는다`() {
        // given
        val oldLogo = upload("old-company-logo", OWNER_ID)
        val newLogo = upload("new-company-logo", OWNER_ID)
        val oldCover = upload("old-bootcamp-cover", OWNER_ID)
        manager.replaceImage(OWNER_ID, null, oldLogo.id, NOW)
        manager.replaceImage(OWNER_ID, null, oldCover.id, NOW)
        job(logoUrl = oldLogo.url)
        bootcamp(representativeImageUrl = oldCover.url)
        flush()

        // when
        manager.replaceImage(OWNER_ID, oldLogo.id, newLogo.id, NOW)
        manager.unreferenceImage(OWNER_ID, oldCover.id, NOW)
        flush()
        val deletedCount = manager.cleanup(NOW.plusDays(2), Duration.ofHours(24))
        flush()

        // then
        assertEquals(0, deletedCount)
        assertEquals(ImageAssetStatus.ATTACHED, repository.findById(oldLogo.id).orElseThrow().status)
        assertEquals(ImageAssetStatus.ATTACHED, repository.findById(oldCover.id).orElseThrow().status)
        Mockito.verify(storage, Mockito.never()).delete(Mockito.anyString())
    }

    @Test
    fun `관리자나 AI가 공고에 넣어 첨부되지 않은 이미지도 정리 작업이 지우지 않고 첨부로 되돌린다`() {
        // given
        val temporary = upload("admin-set-logo", OWNER_ID + 1)
        val notUsed = upload("not-used", OWNER_ID + 1)
        job(logoUrl = "https://example.com/external.png", coverImageUrl = temporary.url)
        flush()

        // when
        val deletedCount = manager.cleanup(LocalDateTime.now().plusDays(2), Duration.ofHours(24))
        flush()

        // then
        assertEquals(1, deletedCount)
        assertEquals(ImageAssetStatus.ATTACHED, repository.findById(temporary.id).orElseThrow().status)
        assertEquals(ImageAssetStatus.DELETED, repository.findById(notUsed.id).orElseThrow().status)
        Mockito.verify(storage, Mockito.never()).delete(temporary.storageKey)
    }

    @Test
    fun `정리 작업이 지운 이미지 URL은 공고에 저장할 수 없고 외부 URL은 저장할 수 있다`() {
        // given
        val deleted = upload("deleted-logo", OWNER_ID)
        manager.cleanup(LocalDateTime.now().plusDays(2), Duration.ofHours(24))
        flush()

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            manager.attachImageUrls(OWNER_ID, listOf(deleted.url, "https://example.com/external.png"))
        }
        manager.attachImageUrls(OWNER_ID, listOf("https://example.com/external.png"))

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
    }

    private fun flush() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun job(logoUrl: String?, coverImageUrl: String? = null): Job =
        Job(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = JobEmploymentType.FULL_TIME,
            experienceType = JobExperienceType.EXPERIENCED,
            experienceMinYears = 1,
            recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
            publicationStatus = JobPublicationStatus.PUBLISHED,
            sourceUrl = "https://example.com/jobs/1",
            listSortKey = 0L,
            now = NOW,
            coverImageUrl = coverImageUrl,
            logoUrl = logoUrl,
        ).also(entityManager::persist)

    private fun bootcamp(representativeImageUrl: String?): Bootcamp =
        Bootcamp(
            companyName = "오공고 교육사",
            title = "백엔드 부트캠프",
            programType = "개발",
            operationType = BootcampOperationType.ONLINE,
            recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
            programStartDate = LocalDate.of(2026, 9, 1),
            programEndDate = LocalDate.of(2026, 12, 1),
            tuitionType = BootcampTuitionType.FREE,
            representativeImageUrl = representativeImageUrl,
            shortDescription = "백엔드 개발자로 성장하는 12주",
            content = "부트캠프 상세 내용",
            applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
            applicationUrl = "https://example.com/apply",
        ).also(entityManager::persist)

    private fun upload(id: String, ownerUserId: Long): ImageAsset = repository.save(
        ImageAsset.uploading(
            id = id,
            ownerUserId = ownerUserId,
            storageKey = "ogonggo/etc/$id.png",
            url = "https://bucket.example.com/ogonggo/etc/$id.png",
            mimeType = "image/png",
            size = 3L,
        ).also { it.markUploaded() },
    )

    companion object {
        private const val OWNER_ID = 81000L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 1, 10, 0)
    }
}
