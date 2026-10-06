package com.ogonggo.userapi.image.business

import com.ogonggo.core.image.implement.ImageAssetManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * 게시글에 쓰이지 않은 채 보존 기간이 지난 업로드 이미지를 지우고 지운 건수를 돌려준다.
 *
 * S3 삭제를 기다리는 동안 DB 커넥션을 잡지 않도록 트랜잭션으로 묶지 않는다.
 * 이미지마다 상태를 바로 저장하므로, 중간에 실패해도 지운 만큼은 남고 나머지는 다음 실행에서 다시 지운다.
 */
@Service
class ImageAssetCleanupService(
    private val imageAssetManager: ImageAssetManager,
    private val clock: Clock,
    @Value("\${ogonggo.storage.s3.cleanup.retention-hours:24}")
    private val retentionHours: Long,
) {

    fun cleanup(): Int = imageAssetManager.cleanup(
        now = LocalDateTime.now(clock),
        retention = Duration.ofHours(retentionHours),
    )
}
