package com.ogonggo.adminapi.recruitmentpost.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.user.implement.UserProfileReader
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AdminRecruitmentPostServiceTest {

    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val postManager = Mockito.mock(RecruitmentPostManager::class.java)
    private val service = AdminRecruitmentPostService(
        postReader,
        postManager,
        Mockito.mock(RecruitmentPostMetricReader::class.java),
        Mockito.mock(UserProfileReader::class.java),
    )

    @Test
    fun `노출 일괄 변경은 잠가 읽은 모집글 중 노출이 다른 것만 바꾼다`() {
        // given
        val hidden = postWithStatus(RecruitmentPostPublicationStatus.HIDDEN)
        val published = postWithStatus(RecruitmentPostPublicationStatus.PUBLISHED)
        Mockito.`when`(postReader.readAllPostedForUpdate(listOf(3L, 1L))).thenReturn(listOf(hidden, published))

        // when
        service.changeVisibilities(
            AdminRecruitmentPostVisibilityChangeCommand(listOf(3L, 1L), AdminContentVisibility.HIDDEN),
        )

        // then
        Mockito.verify(postManager).hide(published)
        Mockito.verifyNoMoreInteractions(postManager)
    }

    @Test
    fun `숨긴 모집글을 노출로 바꾸면 다시 공개한다`() {
        // given
        val hidden = postWithStatus(RecruitmentPostPublicationStatus.HIDDEN)
        Mockito.`when`(postReader.readAllPostedForUpdate(listOf(3L))).thenReturn(listOf(hidden))

        // when
        service.changeVisibilities(
            AdminRecruitmentPostVisibilityChangeCommand(listOf(3L), AdminContentVisibility.VISIBLE),
        )

        // then
        Mockito.verify(postManager).unhide(hidden)
        Mockito.verifyNoMoreInteractions(postManager)
    }

    private fun postWithStatus(publicationStatus: RecruitmentPostPublicationStatus): RecruitmentPost =
        Mockito.mock(RecruitmentPost::class.java).also { post ->
            Mockito.`when`(post.publicationStatus).thenReturn(publicationStatus)
        }
}
