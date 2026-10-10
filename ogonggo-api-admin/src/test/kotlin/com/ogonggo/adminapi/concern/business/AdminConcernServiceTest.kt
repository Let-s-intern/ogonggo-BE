package com.ogonggo.adminapi.concern.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernManager
import com.ogonggo.core.concern.implement.ConcernMetricReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.user.implement.UserProfileReader
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AdminConcernServiceTest {

    private val concernReader = Mockito.mock(ConcernReader::class.java)
    private val concernManager = Mockito.mock(ConcernManager::class.java)
    private val service = AdminConcernService(
        concernReader,
        concernManager,
        Mockito.mock(ConcernMetricReader::class.java),
        Mockito.mock(ConcernCommentReader::class.java),
        Mockito.mock(UserProfileReader::class.java),
    )

    @Test
    fun `노출 일괄 변경은 잠가 읽은 고민글 중 노출이 다른 것만 숨긴다`() {
        // given
        val hidden = concern(hidden = true)
        val visible = concern(hidden = false)
        Mockito.`when`(concernReader.readAllIncludingHiddenForUpdate(listOf(3L, 1L))).thenReturn(listOf(hidden, visible))

        // when
        service.changeVisibilities(AdminConcernVisibilityChangeCommand(listOf(3L, 1L), AdminContentVisibility.HIDDEN))

        // then
        Mockito.verify(concernManager).hide(visible)
        Mockito.verifyNoMoreInteractions(concernManager)
    }

    @Test
    fun `숨긴 고민글을 노출로 바꾸면 다시 내놓는다`() {
        // given
        val hidden = concern(hidden = true)
        Mockito.`when`(concernReader.readAllIncludingHiddenForUpdate(listOf(3L))).thenReturn(listOf(hidden))

        // when
        service.changeVisibilities(AdminConcernVisibilityChangeCommand(listOf(3L), AdminContentVisibility.VISIBLE))

        // then
        Mockito.verify(concernManager).unhide(hidden)
        Mockito.verifyNoMoreInteractions(concernManager)
    }

    private fun concern(hidden: Boolean): Concern =
        Mockito.mock(Concern::class.java).also { concern ->
            Mockito.`when`(concern.hidden).thenReturn(hidden)
        }
}
