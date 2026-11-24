package com.ogonggo.userapi.featureflag.implement

import com.ogonggo.core.featureflag.domain.FeatureFlag
import com.ogonggo.core.featureflag.implement.FeatureFlagReader
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * 공개 기능 플래그 목록을 이 인스턴스의 메모리에 30초 둔다. 화면이 열릴 때마다 오는 요청이 DB를 읽지 않게 한다.
 *
 * 서버가 여러 대라 대마다 따로 두므로, 관리자가 바꾼 값이 모든 서버에 퍼지는 데 최대 30초 걸린다.
 * 읽기에 실패하면 캐시를 바꾸지 않고 오류를 그대로 올리며, 다음 요청이 다시 읽는다.
 */
@Component
class UserFeatureFlagCache(
    private val featureFlagReader: FeatureFlagReader,
    private val clock: Clock,
) {

    @Volatile
    private var cached: CachedFeatureFlags? = null

    fun readAll(): List<FeatureFlag> {
        val now = clock.millis()
        cached?.takeIf { now < it.expiresAtMillis }?.let { return it.featureFlags }
        synchronized(this) {
            cached?.takeIf { now < it.expiresAtMillis }?.let { return it.featureFlags }
            val featureFlags = featureFlagReader.readAll()
            cached = CachedFeatureFlags(featureFlags, expiresAtMillis = now + TTL_MILLIS)
            return featureFlags
        }
    }

    private class CachedFeatureFlags(val featureFlags: List<FeatureFlag>, val expiresAtMillis: Long)

    companion object {
        const val TTL_MILLIS = 30_000L
    }
}
