package com.ogonggo.core.storage.s3

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * 오공고 S3 버킷에 객체를 올리고 복사하고 지우며, 객체를 화면에 보여 줄 URL을 만든다.
 *
 * 어떤 파일인지, 누가 올렸는지는 알지 못한다. key는 호출자가 만들고,
 * 이 클래스는 bucket·URL 조합과 S3 SDK 호출만 담당한다.
 *
 * 올린 객체는 영구 캐시(`immutable`)로 내려간다. 호출자는 같은 key에 다른 내용을 덮어쓰지 않도록
 * 내용마다 고유한 key(UUID, 원본 주소의 해시 등)를 써야 한다.
 */
@Component
class S3ObjectClient(
    private val s3Client: S3Client,
    private val properties: S3Properties,
) {

    /** bucket이 비어 있으면 false다. 이때 [put]·[copy]·[delete]·[urlOf]는 실패한다. */
    fun isConfigured(): Boolean = properties.bucket.isNotBlank()

    fun put(
        key: String,
        content: ByteArray,
        contentType: String,
    ): String {
        checkConfigured()

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build(),
            RequestBody.fromBytes(content),
        )

        return urlOf(key)
    }

    fun delete(key: String) {
        checkConfigured()
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .build(),
        )
    }

    fun copy(sourceKey: String, targetKey: String, contentType: String) {
        checkConfigured()
        s3Client.copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(properties.bucket)
                .sourceKey(sourceKey)
                .destinationBucket(properties.bucket)
                .destinationKey(targetKey)
                .contentType(contentType)
                .build(),
        )
    }

    /** CDN 주소(`publicBaseUrl`)가 있으면 CDN 주소로, 없으면 S3 기본 주소로 [key]의 표시용 URL을 만든다. */
    fun urlOf(key: String): String {
        checkConfigured()
        return properties.publicBaseUrl
            .trimEnd('/')
            .takeIf { it.isNotBlank() }
            ?.let { "$it/$key" }
            ?: "https://${properties.bucket}.s3.${properties.region}.amazonaws.com/$key"
    }

    private fun checkConfigured() {
        check(isConfigured()) { "S3 bucket 설정이 없습니다." }
    }

    private companion object {
        const val CACHE_CONTROL = "public, max-age=31536000, immutable"
    }
}
