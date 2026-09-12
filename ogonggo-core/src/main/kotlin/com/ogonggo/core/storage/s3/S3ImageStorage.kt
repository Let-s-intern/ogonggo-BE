package com.ogonggo.core.storage.s3

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

/**
 * 이미지 바이트를 S3에 저장하고 클라이언트가 사용할 표시용 URL을 반환한다.
 *
 * HTTP 요청이나 사용자 정책은 알지 못한다. key는 호출자가 생성하고,
 * bucket과 URL 조합 및 S3 SDK 호출만 담당한다.
 */
@Component
class S3ImageStorage(
    private val s3Client: S3Client,
    private val properties: S3ImageStorageProperties,
) {

    fun put(
        key: String,
        content: ByteArray,
        contentType: String,
    ): String {
        check(properties.bucket.isNotBlank()) { "S3 bucket 설정이 없습니다." }

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build(),
            RequestBody.fromBytes(content),
        )

        return publicUrl(key)
    }

    fun delete(key: String) {
        check(properties.bucket.isNotBlank()) { "S3 bucket 설정이 없습니다." }
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .build(),
        )
    }

    fun publicUrl(key: String): String {
        check(properties.bucket.isNotBlank()) { "S3 bucket 설정이 없습니다." }
        return properties.publicBaseUrl
            .trimEnd('/')
            .takeIf { it.isNotBlank() }
            ?.let { "$it/$key" }
            ?: "https://${properties.bucket}.s3.${properties.region}.amazonaws.com/$key"
    }

    private companion object {
        const val CACHE_CONTROL = "public, max-age=31536000, immutable"
    }
}
