package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24InstitutionImagesDto
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class Work24InstitutionImageImporterTest {

    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    private val uploadedKeys = mutableListOf<String>()

    @Test
    fun `훈련기관 소개 화면의 로고와 사진을 저장소로 옮기고 같은 기관은 다시 받지 않는다`() {
        // given
        server.expect(requestTo(INSTITUTION_URL)).andRespond(withSuccess(WORK24_INSTITUTION_PAGE_HTML, MediaType.TEXT_HTML))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=LOGO&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(152, 90, "png"), MediaType.APPLICATION_OCTET_STREAM))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=DESK&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(1600, 1200, "jpg"), MediaType.APPLICATION_OCTET_STREAM))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=ROOM&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(800, 600, "jpg"), MediaType.APPLICATION_OCTET_STREAM))
        val importer = importer(bucket = "ogonggo")

        // when
        val images = importer.import("ORG", INSTITUTION_URL)
        val again = importer.import("ORG", "$INSTITUTION_URL&tracseTme=2")

        // then: 두 번째 과정은 고용24를 부르지 않는다.
        server.verify()
        assertEquals(images, again)
        assertEquals(3, uploadedKeys.size)
        assertTrue(uploadedKeys[0].matches(Regex("""images/work24/[0-9a-f]{40}\.png""")), uploadedKeys[0])
        assertTrue(uploadedKeys[1].matches(Regex("""images/work24/[0-9a-f]{40}\.jpg""")), uploadedKeys[1])
        assertEquals(
            Work24InstitutionImagesDto(
                logoUrl = "https://cdn.ogonggo.test/${uploadedKeys[0]}",
                photos = listOf(
                    Work24InstitutionImagesDto.Photo("https://cdn.ogonggo.test/${uploadedKeys[1]}", "안내데스크"),
                    Work24InstitutionImagesDto.Photo("https://cdn.ogonggo.test/${uploadedKeys[2]}", "강의실"),
                ),
            ),
            images,
        )
    }

    @Test
    fun `이미지 하나를 받지 못해도 받은 이미지는 넣는다`() {
        server.expect(requestTo(INSTITUTION_URL)).andRespond(withSuccess(WORK24_INSTITUTION_PAGE_HTML, MediaType.TEXT_HTML))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=LOGO&athfilSeqNo=2"))
            // 고용24는 파일이 없을 때 200과 경고 스크립트를 준다.
            .andRespond(withSuccess("<script>alert('오류');</script>", MediaType.TEXT_HTML))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=DESK&athfilSeqNo=2"))
            .andRespond(withSuccess(work24TestImage(800, 600, "jpg"), MediaType.APPLICATION_OCTET_STREAM))
        server.expect(requestTo("$SITE/hr/z/z/0000/hrdFileDownLoad.do?athfilId=ROOM&athfilSeqNo=2")).andRespond(withServerError())

        val images = requireNotNull(importer(bucket = "ogonggo").import("ORG", INSTITUTION_URL))

        assertNull(images.logoUrl)
        assertEquals(
            listOf(Work24InstitutionImagesDto.Photo("https://cdn.ogonggo.test/${uploadedKeys.single()}", "안내데스크")),
            images.photos,
        )
    }

    @Test
    fun `소개 화면을 읽지 못하거나 저장소가 설정되지 않았으면 이미지를 넣지 않는다`() {
        server.expect(requestTo(INSTITUTION_URL)).andRespond(withServerError())

        assertNull(importer(bucket = "ogonggo").import("ORG", INSTITUTION_URL))
        // 저장소가 없으면 고용24를 부르지 않는다.
        assertNull(importer(bucket = "").import("ORG", INSTITUTION_URL))

        server.verify()
        assertTrue(uploadedKeys.isEmpty())
    }

    private fun importer(bucket: String) = Work24InstitutionImageImporter(
        restClientBuilder.build(),
        s3ObjectClient(configured = bucket.isNotBlank()),
    )

    /** S3 대신 올린 키를 기록하고 공개 주소를 돌려준다. */
    private fun s3ObjectClient(configured: Boolean) = Mockito.mock(S3ObjectClient::class.java) { invocation ->
        if (invocation.method.name == "isConfigured") {
            return@mock configured
        }
        val key = invocation.getArgument<String>(0)
        uploadedKeys += key
        "https://cdn.ogonggo.test/$key"
    }

    private companion object {
        const val SITE = "https://www.work24.go.kr"
        const val INSTITUTION_URL = "$SITE/hr/a/a/3200/selectTrainInstitution.do?tracseId=C101&trainstCstmrId=ORG"
    }
}

/** 고용24 훈련기관 소개 화면에서 읽는 부분만 남긴 HTML이다. 화살표 같은 화면 장식 이미지는 옮기지 않아야 한다. */
internal val WORK24_INSTITUTION_PAGE_HTML = """
    <html><body>
    <strong class="title">오공고 아카데미</strong>
    <div class="box_btn_group">
      <img src="/hr/z/z/0000/hrdFileDownLoad.do?athfilId=LOGO&amp;athfilSeqNo=2" width="152px" height="92px" alt="훈련기관사진" title="훈련기관사진">
    </div>
    <button class="swiper-button-prev"><img src="/cm/static/images/ico16_arrow_left.svg" alt=""></button>
    <div class="thumbnailIntroList swiper"><div class="list swiper-wrapper">
      <div class="swiper-slide">
        <div class="thumb"><img src="/hr/z/z/0000/hrdFileDownLoad.do?athfilId=DESK&amp;athfilSeqNo=2" alt=""></div><p>안내데스크</p>
      </div>
      <div class="swiper-slide">
        <div class="thumb"><img src="/hr/z/z/0000/hrdFileDownLoad.do?athfilId=ROOM&amp;athfilSeqNo=2" alt=""></div><p>강의실</p>
      </div>
    </div></div>
    </body></html>
""".trimIndent()
