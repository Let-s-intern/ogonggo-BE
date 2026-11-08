package com.ogonggo.adminapi.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Api
import com.ogonggo.adminapi.ingestion.work24.presentation.pathName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-admin-openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
    ],
)
@AutoConfigureMockMvc
class AdminOpenApiContractTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    /** 사용자 API와 같은 이유로 operationId를 코드에서 정하고, 겹치거나 자동 접미사가 붙으면 빌드를 깨뜨린다. */
    @Test
    fun `모든 operationId는 유일하며 자동 생성 접미사가 붙지 않는다`() {
        val document = openApiDocument()

        val operationIds = document.at("/paths").fields().asSequence()
            .flatMap { (path, operations) ->
                operations.fields().asSequence().map { (method, operation) ->
                    "$method $path" to operation.at("/operationId").asText()
                }
            }
            .toList()

        assertTrue(operationIds.none { (_, id) -> id.isEmpty() }, "operationId가 없는 작업: $operationIds")
        assertTrue(operationIds.none { (_, id) -> id.matches(Regex(".*_\\d+$")) }, "접미사가 붙은 작업: $operationIds")
        assertTrue(operationIds.groupBy { it.second }.all { it.value.size == 1 }, "operationId가 겹칩니다: $operationIds")
    }

    @Test
    fun `관리자 콘솔 API는 Bearer 인증을 요구하고 목록 기본 크기는 20이다`() {
        val document = openApiDocument()

        val jobs = document.at("/paths/~1api~1v1~1admin~1jobs/get")
        assertTrue(jobs.at("/security/0/BearerAuth").isArray)
        assertEquals(
            "20",
            jobs.at("/parameters").first { it.at("/name").asText() == "size" }.at("/schema/default").asText(),
        )
        listOf(
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}/patch",
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}/delete",
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}~1inspection/put",
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}~1inspection/delete",
            "/paths/~1api~1v1~1admin~1jobs~1today/get",
            "/paths/~1api~1v1~1admin~1jobs~1today/put",
            "/paths/~1api~1v1~1admin~1bootcamps/get",
            "/paths/~1api~1v1~1admin~1bootcamps~1{bootcampId}/patch",
            "/paths/~1api~1v1~1admin~1recruitment-posts/get",
            "/paths/~1api~1v1~1admin~1recruitment-posts~1visibility/patch",
            "/paths/~1api~1v1~1admin~1concerns/get",
            "/paths/~1api~1v1~1admin~1concerns~1{concernId}/get",
            "/paths/~1api~1v1~1admin~1concerns~1visibility/patch",
            "/paths/~1api~1v1~1admin~1content-reviews/get",
            "/paths/~1api~1v1~1admin~1content-reviews~1{type}~1{id}/patch",
            "/paths/~1api~1v1~1admin~1content-reviews~1{type}~1{id}~1undo/patch",
            "/paths/~1api~1v1~1admin~1content-rejections/get",
            "/paths/~1api~1v1~1admin~1content-rejections~1{type}~1{id}/patch",
            "/paths/~1api~1v1~1admin~1announcements/get",
            "/paths/~1api~1v1~1admin~1announcements/post",
            "/paths/~1api~1v1~1admin~1announcements~1{announcementId}/get",
            "/paths/~1api~1v1~1admin~1announcements~1{announcementId}/patch",
            "/paths/~1api~1v1~1admin~1announcements~1{announcementId}/delete",
            "/paths/~1api~1v1~1admin~1work24~1{apiName}/get",
            "/paths/~1api~1v1~1admin~1service-feedbacks/get",
            "/paths/~1api~1v1~1admin~1general-members/get",
            "/paths/~1api~1v1~1admin~1company-members/get",
            "/paths/~1api~1v1~1admin~1general-members~1{userId}/get",
            "/paths/~1api~1v1~1admin~1company-members~1{userId}/get",
            "/paths/~1api~1v1~1admin~1advertisement-inquiries/get",
            "/paths/~1api~1v1~1admin~1advertisement-inquiries~1today-count/get",
            "/paths/~1api~1v1~1admin~1advertisement-inquiries~1{inquiryId}~1status/patch",
            "/paths/~1api~1v1~1admin~1advertisement-inquiries~1{inquiryId}~1memo/patch",
            "/paths/~1api~1v1~1admin~1company-content-priority/get",
            "/paths/~1api~1v1~1admin~1company-content-priority/put",
        ).forEach { pointer ->
            assertTrue(document.at("$pointer/security/0/BearerAuth").isArray, "인증 명세가 없습니다: $pointer")
        }
    }

    @Test
    fun `AI 서버 내부 API는 내부 API 키 인증을 요구한다`() {
        val document = openApiDocument()

        listOf(
            "/paths/~1api~1v1~1internal~1jobs/post",
            "/paths/~1api~1v1~1internal~1jobs/get",
            "/paths/~1api~1v1~1internal~1jobs~1{jobId}/put",
            "/paths/~1api~1v1~1internal~1jobs~1{jobId}/delete",
            "/paths/~1api~1v1~1internal~1bootcamps/post",
            "/paths/~1api~1v1~1internal~1bootcamps/get",
            "/paths/~1api~1v1~1internal~1bootcamps~1{bootcampId}/put",
            "/paths/~1api~1v1~1internal~1bootcamps~1{bootcampId}/delete",
        ).forEach { pointer ->
            assertTrue(
                document.at("$pointer/security/0/$ADMIN_INTERNAL_API_KEY_SCHEME").isArray,
                "내부 API 키 인증 명세가 없습니다: $pointer",
            )
        }
    }

    @Test
    fun `AI 서버 부트캠프 등록은 중복 원문을 409 BOOTCAMP_ALREADY_EXISTS로 명시한다`() {
        val document = openApiDocument()

        val conflict = document.at("/paths/~1api~1v1~1internal~1bootcamps/post/responses/409/description").asText()
        assertTrue(conflict.startsWith("BOOTCAMP_ALREADY_EXISTS"), "409 명세가 없습니다: $conflict")
        assertTrue(document.at("/paths/~1api~1v1~1internal~1bootcamps~1{bootcampId}/delete/responses/404").isObject)
    }

    @Test
    fun `AI 서버 부트캠프 요청의 모집 상태는 선택 칸이며 모집 중과 모집 마감만 받는다`() {
        val document = openApiDocument()

        val schema = document.at("/components/schemas/AiBootcampRequest")
        val statusSchema = schema.at("/properties/status")
        assertEquals(listOf("RECRUITING", "CLOSED"), statusSchema.at("/enum").map { it.asText() })
        // 값 설명 표도 받는 값만 싣는다.
        assertFalse(statusSchema.at("/description").asText().contains("`DRAFT`"), "받지 않는 값이 설명에 있습니다.")
        assertTrue(statusSchema.at("/description").asText().contains("`CLOSED`"), "값 설명 표가 없습니다.")
        assertFalse(schema.at("/required").any { it.asText() == "status" }, "모집 상태가 필수로 잡혀 있습니다.")
        listOf(
            "/paths/~1api~1v1~1internal~1bootcamps/post/description",
            "/paths/~1api~1v1~1internal~1bootcamps~1{bootcampId}/put/description",
        ).forEach { pointer ->
            assertTrue(document.at(pointer).asText().contains("status"), "모집 상태 설명이 없습니다: $pointer")
        }
    }

    @Test
    fun `고용24 조회의 apiName 목록은 지원하는 고용24 API와 같다`() {
        val document = openApiDocument()

        val apiName = document.at("/paths/~1api~1v1~1admin~1work24~1{apiName}/get/parameters")
            .first { it.at("/name").asText() == "apiName" }
        assertEquals(
            Work24Api.entries.map { it.pathName },
            apiName.at("/schema/enum").map { it.asText() },
        )
    }

    @Test
    fun `AI 서버 공고 등록과 교체는 원문을 받고 원문만 바꾸는 API가 있다`() {
        val document = openApiDocument()

        listOf("AiJobRegistrationRequest", "AiJobReplaceRequest").forEach { name ->
            assertTrue(
                document.at("/components/schemas/$name/properties/sourceHtml").isObject,
                "$name 에 sourceHtml 이 없습니다.",
            )
        }
        assertTrue(document.at("/paths/~1api~1v1~1internal~1jobs~1{jobId}~1source-html/put").isObject)
    }

    @Test
    fun `FCM 발송 API는 관리자 인증과 적재 응답 및 요청 길이를 명세한다`() {
        // given
        val document = openApiDocument()
        val create = document.at("/paths/~1api~1v1~1admin~1notification-batches/post")
        val result = document.at("/paths/~1api~1v1~1admin~1notification-batches~1{batchId}/get")
        val request = document.at("/components/schemas/CreateNotificationBatchRequest")

        // when, then
        assertEquals("createNotificationBatch", create.at("/operationId").asText())
        assertEquals("getNotificationBatch", result.at("/operationId").asText())
        assertTrue(create.at("/security/0/BearerAuth").isArray)
        assertTrue(result.at("/security/0/BearerAuth").isArray)
        assertTrue(create.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(result.at("/responses/200/content/application~1json/schema").isObject)
        val created = document.at("/components/schemas/AdminNotificationBatchCreatedResponse/properties")
        assertEquals(setOf("DIRECT", "TOPIC"), created.at("/deliveryTargetType/enum").map { it.asText() }.toSet())
        assertTrue(created.has("noTokenUserCount"))
        assertTrue(created.at("/skippedNoTokenUserCount/deprecated").asBoolean())
        assertEquals(100, request.at("/properties/title/maxLength").asInt())
        assertEquals(1000, request.at("/properties/body/maxLength").asInt())
        assertEquals("string", request.at("/properties/data/additionalProperties/type").asText())
        assertEquals("uuid", result.at("/parameters").first { it.at("/name").asText() == "batchId" }.at("/schema/format").asText())
        assertTrue(result.at("/responses/404/description").asText().contains("NOTIFICATION_BATCH_NOT_FOUND"))
        assertEquals("uuid", request.at("/properties/imageId/format").asText())
        assertTrue(document.at("/components/schemas/AdminNotificationBatchResultResponse/properties").has("imageUrl"))
        val statistics = document.at("/paths/~1api~1v1~1admin~1notification-batches~1statistics/get")
        assertEquals("getNotificationBatchStatistics", statistics.at("/operationId").asText())
        assertTrue(statistics.at("/security/0/BearerAuth").isArray)
        assertTrue(document.at("/paths/~1api~1v1~1admin~1notification-images/post").isMissingNode)
    }

    @Test
    @DisplayName("이메일 재발송은 관리자 인증·양수 회원 ID·적재 응답과 수신자 오류를 명세한다")
    fun `가입 환영 이메일 재발송 계약을 제공한다`() {
        // given
        val document = openApiDocument()

        // when
        val operation = document.at("/paths/~1api~1v1~1admin~1general-members~1{userId}~1welcome-email-resends/post")
        val userId = operation.at("/parameters").first { it["name"].asText() == "userId" }
        val response = document.at("/components/schemas/AdminWelcomeEmailResendResponse/properties")

        // then
        assertEquals("createWelcomeEmailResend", operation.at("/operationId").asText())
        assertTrue(operation.at("/security/0/BearerAuth").isArray)
        assertTrue(userId.at("/schema/minimum").asDouble() > 0)
        assertTrue(operation.at("/requestBody").isMissingNode)
        assertTrue(operation.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(operation.at("/responses/409/description").asText().contains("WELCOME_EMAIL_RECIPIENT_UNAVAILABLE"))
        assertTrue(response.has("notificationId"))
        assertTrue(response.has("recipientUserId"))
        assertTrue(response.has("scheduledAt"))
        assertFalse(response.has("recipientAddress"))
    }

    @Test
    fun `회원 상세는 목록 항목의 필드를 같은 높이에 펼치고 활동 목록과 없는 회원 오류를 명세한다`() {
        // given
        val document = openApiDocument()

        // when
        val general = document.at("/paths/~1api~1v1~1admin~1general-members~1{userId}/get")
        val company = document.at("/paths/~1api~1v1~1admin~1company-members~1{userId}/get")
        val generalDetail = document.at("/components/schemas/AdminGeneralMemberDetailResponse/properties")
        val companyDetail = document.at("/components/schemas/AdminCompanyMemberDetailResponse/properties")

        // then
        assertEquals("getGeneralMember", general.at("/operationId").asText())
        assertEquals("getCompanyMember", company.at("/operationId").asText())
        listOf(general, company).forEach { operation ->
            val userId = operation.at("/parameters").first { it["name"].asText() == "userId" }
            assertTrue(userId.at("/schema/minimum").asDouble() > 0)
            assertTrue(operation.at("/responses/404/description").asText().startsWith("USER_NOT_FOUND"))
        }
        listOf("userId", "nickname", "role", "bookmarkedJobs", "bookmarkedBootcamps", "authoredSideStudies").forEach { field ->
            assertTrue(generalDetail.has(field), field)
        }
        listOf("userId", "organizationName", "businessNumber", "recommender", "jobs").forEach { field ->
            assertTrue(companyDetail.has(field), field)
        }
        assertFalse(generalDetail.has("member"))
        assertFalse(companyDetail.has("member"))
    }

    @Test
    fun `광고 문의 목록의 처리 상태 필터는 여러 값을 받는 배열이다`() {
        // given
        val document = openApiDocument()

        // when
        val status = document.at("/paths/~1api~1v1~1admin~1advertisement-inquiries/get/parameters")
            .first { it["name"].asText() == "status" }

        // then
        assertEquals("array", status.at("/schema/type").asText())
        assertEquals(
            listOf("RECEIVED", "CONTACTED", "EXECUTED", "REJECTED"),
            status.at("/schema/items/enum").map { it.asText() },
        )
        assertFalse(status.at("/required").asBoolean())
    }

    @Test
    fun `광고 상품 관리 API는 관리자 인증과 생성 수정 삭제 계약을 명시한다`() {
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1admin~1advertisement-products"
        val list = document.at("$base/get")
        val create = document.at("$base/post")
        val replace = document.at("$base~1{productId}/put")
        val delete = document.at("$base~1{productId}/delete")
        val createRequest = document.at("/components/schemas/CreateAdminAdvertisementProductRequest")
        val replaceRequest = document.at("/components/schemas/ReplaceAdminAdvertisementProductRequest")
        val response = document.at("/components/schemas/AdminAdvertisementProductResponse")

        assertEquals("listAdvertisementProducts", list.at("/operationId").asText())
        assertEquals("createAdvertisementProduct", create.at("/operationId").asText())
        assertEquals("replaceAdvertisementProduct", replace.at("/operationId").asText())
        assertEquals("deleteAdvertisementProduct", delete.at("/operationId").asText())
        listOf(list, create, replace, delete).forEach { operation ->
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertEquals("UNAUTHORIZED", operation.at("/responses/401/description").asText())
            assertTrue(operation.at("/responses/403/description").asText().startsWith("FORBIDDEN"))
            listOf("401", "403").forEach { statusCode ->
                assertEquals(
                    "#/components/schemas/ErrorResponse",
                    operation.at("/responses/$statusCode/content/application~1json/schema/\$ref").asText(),
                )
            }
        }
        assertTrue(create.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(create.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(replace.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
        assertTrue(
            replace.at("/responses/400/description").asText().contains("ADVERTISEMENT_PRODUCT_POSITION_MISMATCH"),
        )
        assertTrue(delete.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
        listOf(createRequest, replaceRequest).forEach { request ->
            assertEquals(setOf("category", "name"), request.at("/required").map { it.asText() }.toSet())
            assertEquals(100, request.at("/properties/name/maxLength").asInt())
            assertEquals(0, request.at("/properties/price/minimum").asInt())
            assertTrue(request.at("/properties/position").isObject)
            assertTrue(request.at("/properties/price").isObject)
            assertTrue(request.at("/properties/position/nullable").asBoolean())
            assertTrue(request.at("/properties/price/nullable").asBoolean())
        }
        listOf("id", "category", "position", "name", "price", "registeredAt", "updatedAt").forEach { field ->
            assertTrue(response.at("/properties/$field").isObject, field)
        }
        assertTrue(response.at("/properties/position/nullable").asBoolean())
        assertTrue(response.at("/properties/price/nullable").asBoolean())
    }

    @Test
    fun `광고 계약 목록과 상세는 관리자 인증 필터 페이지와 연결 상품 계약을 명시한다`() {
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1admin~1advertisement-contracts"
        val list = document.at("$base/get")
        val detail = document.at("$base~1{contractId}/get")
        val summary = document.at("/components/schemas/AdminAdvertisementContractSummaryResponse")
        val detailSchema = document.at("/components/schemas/AdminAdvertisementContractDetailResponse")
        val product = document.at("/components/schemas/AdminAdvertisementContractProductResponse")

        assertEquals("listAdvertisementContracts", list.at("/operationId").asText())
        assertEquals("getAdvertisementContract", detail.at("/operationId").asText())
        listOf(list, detail).forEach { operation ->
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/401/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
            assertTrue(operation.at("/responses/403/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
        }
        val page = list.at("/parameters").first { it.at("/name").asText() == "page" }.at("/schema")
        val size = list.at("/parameters").first { it.at("/name").asText() == "size" }.at("/schema")
        assertEquals("1", page.at("/default").asText())
        assertEquals(1, page.at("/minimum").asInt())
        assertEquals("20", size.at("/default").asText())
        assertEquals(1, size.at("/minimum").asInt())
        assertEquals(100, size.at("/maximum").asInt())
        val statuses = list.at("/parameters").first { it.at("/name").asText() == "status" }
        assertEquals("array", statuses.at("/schema/type").asText())
        assertEquals(
            setOf("REQUESTED", "NEGOTIATING", "APPROVED", "REJECTED"),
            statuses.at("/schema/items/enum").map { it.asText() }.toSet(),
        )
        assertTrue(list.at("/description").asText().contains("AND"))
        listOf("createdFrom", "createdTo").forEach { name ->
            assertEquals("date", list.at("/parameters").first { it.at("/name").asText() == name }.at("/schema/format").asText(), name)
        }
        assertEquals(100, list.at("/parameters").first { it.at("/name").asText() == "companyName" }.at("/schema/maxLength").asInt())
        assertTrue(detail.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertEquals(
            "#/components/schemas/ErrorResponse",
            detail.at("/responses/400/content/application~1json/schema/\$ref").asText(),
        )
        assertTrue(detail.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        listOf("source", "status", "promotionTarget", "targetId", "companyName", "recruitmentTitle", "recruitmentDeadline", "categories", "paymentAmount", "registeredAt", "updatedAt").forEach { field ->
            assertTrue(summary.at("/properties/$field").isObject, field)
        }
        listOf("userId", "recruitmentUrl", "purpose", "scheduleRequest", "managerName", "email", "invoiceMethod", "creativeMethod", "products").forEach { field ->
            assertTrue(detailSchema.at("/properties/$field").isObject, field)
        }
        assertTrue(detailSchema.at("/properties/products/items").isObject, detailSchema.at("/properties/products").toPrettyString())
        listOf("contractProductId", "productId", "productName", "category", "productDeleted", "addedAt").forEach { field ->
            assertTrue(product.at("/properties/$field").isObject, field)
        }
    }

    @Test
    fun `광고 계약 운영 OpenAPI는 상태 금액 상품 연결·제거의 인증과 오류 응답을 명시한다`() {
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1admin~1advertisement-contracts~1{contractId}"
        val status = document.at("$base~1status/patch")
        val payment = document.at("$base~1payment-amount/patch")
        val addProduct = document.at("$base~1products/post")
        val removeProduct = document.at("$base~1products~1{contractProductId}/delete")

        assertEquals("updateAdvertisementContractStatus", status.at("/operationId").asText())
        assertEquals("updateAdvertisementContractPaymentAmount", payment.at("/operationId").asText())
        assertEquals("createAdvertisementContractProduct", addProduct.at("/operationId").asText())
        assertEquals("deleteAdvertisementContractProduct", removeProduct.at("/operationId").asText())
        listOf(status, payment, addProduct, removeProduct).forEach { operation ->
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/400/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
            assertTrue(operation.at("/responses/401/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
            assertTrue(operation.at("/responses/403/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
        }
        assertTrue(status.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        assertTrue(status.at("/responses/409/description").asText().startsWith("ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW"))
        assertTrue(payment.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        assertTrue(addProduct.at("/responses/201").isObject)
        assertTrue(addProduct.at("/responses/404/description").asText().contains("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
        assertTrue(removeProduct.at("/responses/404/description").asText().contains("ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND"))
        assertEquals(
            listOf("contractId"),
            status.at("/parameters").map { it.at("/name").asText() },
        )
        assertEquals(
            listOf("contractId", "contractProductId"),
            removeProduct.at("/parameters").map { it.at("/name").asText() },
        )

        val paymentRequest = document.at("/components/schemas/ChangeAdminAdvertisementContractPaymentAmountRequest")
        val statusRequest = document.at("/components/schemas/ChangeAdminAdvertisementContractStatusRequest")
        val addProductRequest = document.at("/components/schemas/AddAdminAdvertisementContractProductRequest")
        assertEquals("integer", paymentRequest.at("/properties/paymentAmount/type").asText())
        assertEquals(0, paymentRequest.at("/properties/paymentAmount/minimum").asInt())
        assertTrue(paymentRequest.at("/properties/paymentAmount/nullable").asBoolean())
        assertTrue(paymentRequest.at("/required").any { it.asText() == "paymentAmount" })
        assertEquals("string", statusRequest.at("/properties/status/type").asText())
        assertEquals(
            setOf("REQUESTED", "NEGOTIATING", "APPROVED", "REJECTED"),
            statusRequest.at("/properties/status/enum").map { it.asText() }.toSet(),
        )
        assertEquals("integer", addProductRequest.at("/properties/productId/type").asText())
        assertEquals("int64", addProductRequest.at("/properties/productId/format").asText())
        assertTrue(addProduct.at("/responses/201/content/application~1json/schema/\$ref").asText().endsWith("CreateAdminAdvertisementContractProductResponse"))
    }

    @Test
    fun `관리자 노출 목록과 등록 OpenAPI는 필터 요청 검증 인증 응답 오류를 명시한다`() {
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1admin~1advertisement-exposures"
        val list = document.at("$base/get")
        val create = document.at("$base/post")
        val request = document.at("/components/schemas/CreateAdminAdvertisementExposureRequest")
        val response = document.at("/components/schemas/AdminAdvertisementExposureResponse")
        val createResponse = document.at("/components/schemas/CreateAdminAdvertisementExposureResponse")

        assertEquals("listAdvertisementExposures", list.at("/operationId").asText())
        assertEquals("createAdvertisementExposure", create.at("/operationId").asText())
        listOf(list, create).forEach { operation ->
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            listOf("401", "403").forEach { statusCode ->
                assertTrue(operation.at("/responses/$statusCode/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
            }
        }
        listOf(list, create).forEach { operation ->
            assertTrue(operation.at("/responses/400/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
        }
        assertEquals(listOf("productId", "contractId", "page", "size"), list.at("/parameters").map { it.at("/name").asText() })
        val productId = list.at("/parameters").first { it.at("/name").asText() == "productId" }
        val contractId = list.at("/parameters").first { it.at("/name").asText() == "contractId" }
        assertTrue(productId.at("/required").asBoolean())
        assertEquals("integer", productId.at("/schema/type").asText())
        assertEquals("int64", productId.at("/schema/format").asText())
        assertFalse(contractId.at("/required").asBoolean())
        assertEquals("integer", contractId.at("/schema/type").asText())
        assertEquals("int64", contractId.at("/schema/format").asText())
        assertTrue(list.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(create.at("/responses/201").isObject)
        assertTrue(create.at("/responses/400/description").asText().contains("ADVERTISEMENT_EXPOSURE_PERIOD_INVALID"))
        assertTrue(create.at("/responses/404/description").asText().contains("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        assertTrue(
            create.at("/responses/201/content/application~1json/schema/\$ref").asText()
                .endsWith("CreateAdminAdvertisementExposureResponse"),
        )
        assertEquals("integer", createResponse.at("/properties/id/type").asText())
        assertEquals("int64", createResponse.at("/properties/id/format").asText())
        assertTrue(createResponse.at("/required").any { it.asText() == "id" })

        assertEquals(setOf("productId", "adLabel", "exposed"), request.at("/required").map { it.asText() }.toSet())
        assertEquals("integer", request.at("/properties/productId/type").asText())
        assertEquals("int64", request.at("/properties/contractId/format").asText())
        assertTrue(request.at("/properties/contractId/nullable").asBoolean())
        assertEquals(100, request.at("/properties/name/maxLength").asInt())
        assertEquals(2048, request.at("/properties/imageUrl/maxLength").asInt())
        assertEquals(2048, request.at("/properties/linkUrl/maxLength").asInt())
        assertEquals(setOf("JOB", "BOOTCAMP"), request.at("/properties/targetType/enum").map { it.asText() }.toSet())
        assertEquals("boolean", request.at("/properties/adLabel/type").asText())
        assertEquals("boolean", request.at("/properties/exposed/type").asText())
        listOf(
            "id", "productId", "contractId", "name", "imageUrl", "linkUrl", "targetType", "targetId", "adLabel",
            "startAt", "endAt", "exposed", "advertiserCompanyName", "displayOrder", "registeredAt", "updatedAt",
        ).forEach { field -> assertTrue(response.at("/properties/$field").isObject, field) }
        listOf(
            "contractId", "advertiserCompanyName", "name", "imageUrl", "linkUrl", "targetType", "targetId",
            "startAt", "endAt",
        ).forEach { field -> assertTrue(response.at("/properties/$field/nullable").asBoolean(), field) }
    }

    @Test
    fun `관리자 노출 수정 상태 이동 삭제 OpenAPI는 경로 인증 요청 오류와 성공 계약을 명시한다`() {
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1admin~1advertisement-exposures"
        val replace = document.at("$base~1{exposureId}/put")
        val exposed = document.at("$base~1{exposureId}~1exposed/patch")
        val move = document.at("$base~1{exposureId}~1move/post")
        val delete = document.at("$base~1{exposureId}/delete")
        val replaceRequest = document.at("/components/schemas/UpdateAdminAdvertisementExposureRequest")
        val exposedRequest = document.at("/components/schemas/ChangeAdminAdvertisementExposureExposedRequest")
        val moveRequest = document.at("/components/schemas/MoveAdminAdvertisementExposureRequest")

        assertEquals("replaceAdvertisementExposure", replace.at("/operationId").asText())
        assertEquals("updateAdvertisementExposureExposed", exposed.at("/operationId").asText())
        assertEquals("moveAdvertisementExposure", move.at("/operationId").asText())
        assertEquals("deleteAdvertisementExposure", delete.at("/operationId").asText())
        listOf(replace, exposed, move, delete).forEach { operation ->
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/200").isObject)
            assertTrue(
                operation.at("/responses/400/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"),
            )
            val successSchema = operation.at("/responses/200/content/application~1json/schema")
            val successSchemaRef = successSchema.at("/\$ref").asText()
            assertTrue(successSchemaRef.startsWith("#/components/schemas/"), "성공 응답 래퍼 참조가 없습니다: $successSchemaRef")
            val successResponse = document.at("/components/schemas/${successSchemaRef.substringAfterLast("/")}")
            assertEquals(setOf("status", "message", "data"), successResponse.at("/properties").fieldNames().asSequence().toSet())
            assertEquals("integer", successResponse.at("/properties/status/type").asText())
            assertEquals("string", successResponse.at("/properties/message/type").asText())
            assertTrue(successResponse.at("/properties/data").isObject)
            assertFalse(successResponse.at("/required").any { it.asText() == "data" }, "성공 응답의 nullable payload는 OpenAPI에서 필수로 고정하지 않습니다.")
            listOf("401", "403").forEach { statusCode ->
                assertTrue(operation.at("/responses/$statusCode/content/application~1json/schema/\$ref").asText().endsWith("ErrorResponse"))
            }
            val exposureId = operation.at("/parameters").first { it.at("/name").asText() == "exposureId" }
            assertEquals("integer", exposureId.at("/schema/type").asText())
            assertEquals("int64", exposureId.at("/schema/format").asText())
        }
        assertTrue(replace.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(replace.at("/responses/404/description").asText().contains("ADVERTISEMENT_EXPOSURE_NOT_FOUND"))
        assertTrue(replace.at("/responses/404/description").asText().contains("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
        assertTrue(replace.at("/responses/404/description").asText().contains("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        assertTrue(exposed.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_EXPOSURE_NOT_FOUND"))
        assertTrue(move.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_EXPOSURE_NOT_FOUND"))
        assertTrue(delete.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(delete.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_EXPOSURE_NOT_FOUND"))
        listOf(replace, exposed, move, delete).forEach { operation ->
            assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
        }

        assertEquals(setOf("adLabel"), replaceRequest.at("/required").map { it.asText() }.toSet())
        assertFalse(replaceRequest.has("productId"))
        assertFalse(replaceRequest.has("displayOrder"))
        assertFalse(replaceRequest.has("exposed"))
        assertEquals(100, replaceRequest.at("/properties/name/maxLength").asInt())
        assertEquals(2048, replaceRequest.at("/properties/imageUrl/maxLength").asInt())
        assertEquals(2048, replaceRequest.at("/properties/linkUrl/maxLength").asInt())
        assertTrue(replaceRequest.at("/properties/contractId/nullable").asBoolean())
        assertEquals(setOf("JOB", "BOOTCAMP"), replaceRequest.at("/properties/targetType/enum").map { it.asText() }.toSet())
        assertEquals("boolean", replaceRequest.at("/properties/adLabel/type").asText())
        assertEquals(setOf("exposed"), exposedRequest.at("/required").map { it.asText() }.toSet())
        assertEquals("boolean", exposedRequest.at("/properties/exposed/type").asText())
        assertEquals(setOf("direction"), moveRequest.at("/required").map { it.asText() }.toSet())
        assertEquals(setOf("UP", "DOWN"), moveRequest.at("/properties/direction/enum").map { it.asText() }.toSet())
        assertTrue(delete.at("/requestBody").isMissingNode)
    }

    @Test
    fun `비즈니스 회원 목록은 가입 때 받은 기업·담당자 정보를 응답에 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val properties = document.at("/components/schemas/AdminCompanyMemberResponse/properties")

        // then
        listOf(
            "managerPhone", "businessNumberType", "businessNumber", "companyType", "employeeCountRange", "industry",
            "address", "homepageUrl", "managerTitle", "managerDuty", "recommender",
        ).forEach { field -> assertTrue(properties.has(field), field) }
    }

    private fun openApiDocument() = objectMapper.readTree(
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString,
    )

    @Test
    fun `채용공고 검수 완료는 본문 없는 PUT과 DELETE이고 목록과 상세에 검수 완료 일시를 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val mark = document.at("/paths/~1api~1v1~1admin~1jobs~1{jobId}~1inspection/put")
        val clear = document.at("/paths/~1api~1v1~1admin~1jobs~1{jobId}~1inspection/delete")

        // then
        assertEquals("replaceJobInspection", mark.at("/operationId").asText())
        assertEquals("deleteJobInspection", clear.at("/operationId").asText())
        listOf(mark, clear).forEach { operation ->
            assertTrue(operation.at("/requestBody").isMissingNode)
            assertEquals(listOf("jobId"), operation.at("/parameters").map { it["name"].asText() })
            assertTrue(operation.at("/responses/404/description").asText().startsWith("JOB_NOT_FOUND"))
        }
        assertTrue(document.at("/components/schemas/AdminJobSummaryResponse/properties/inspectedAt").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1admin~1jobs~1{jobId}~1analysis/put/security/0/BearerAuth").isArray)
        assertTrue(document.at("/components/schemas/AdminJobDetailResponse/properties/inspectedAt").isObject)
    }

    @Test
    fun `채용공고 목록 항목은 원문 주소를 nullable 문자열로 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val schema = document.at("/components/schemas/AdminJobSummaryResponse")

        // then
        assertEquals("string", schema.at("/properties/sourceUrl/type").asText())
        val required = schema.at("/required").map { it.asText() }
        assertTrue(required.contains("id"))
        assertFalse(required.contains("sourceUrl"))
    }

    @Test
    fun `채용공고와 부트캠프 수정은 공고별 우선 노출 여부를 선택 칸으로 받고 목록과 상세는 늘 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val jobPatch = document.at("/paths/~1api~1v1~1admin~1jobs~1{jobId}/patch")
        val bootcampPatch = document.at("/paths/~1api~1v1~1admin~1bootcamps~1{bootcampId}/patch")

        // then
        assertEquals(
            "#/components/schemas/UpdateAdminJobRequest",
            jobPatch.at("/requestBody/content/application~1json/schema/\$ref").asText(),
        )
        assertEquals(
            "#/components/schemas/UpdateAdminBootcampRequest",
            bootcampPatch.at("/requestBody/content/application~1json/schema/\$ref").asText(),
        )
        listOf("UpdateAdminJobRequest", "UpdateAdminBootcampRequest").forEach { name ->
            val schema = document.at("/components/schemas/$name")
            assertEquals("boolean", schema.at("/properties/priorityExposed/type").asText(), name)
            assertFalse(schema.at("/required").any { it.asText() == "priorityExposed" }, name)
        }
        listOf(
            "AdminJobSummaryResponse",
            "AdminJobDetailResponse",
            "AdminBootcampSummaryResponse",
            "AdminBootcampDetailResponse",
        ).forEach { name ->
            val schema = document.at("/components/schemas/$name")
            assertEquals("boolean", schema.at("/properties/priorityExposed/type").asText(), name)
            assertTrue(schema.at("/required").any { it.asText() == "priorityExposed" }, name)
        }
    }

    @Test
    fun `비즈니스 회원 공고 대리 등록은 회원 하위 컬렉션에 201로 상세를 돌려주고 목록과 상세에 대리 등록 기록을 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val operations = mapOf(
            "jobs" to Triple("createCompanyMemberJob", "CreateProxyJobRequest", "AdminJobDetailResponse"),
            "bootcamps" to Triple("createCompanyMemberBootcamp", "CreateProxyBootcampRequest", "AdminBootcampDetailResponse"),
        )

        // then
        operations.forEach { (collection, contract) ->
            val (operationId, request, response) = contract
            val operation = document.at("/paths/~1api~1v1~1admin~1company-members~1{userId}~1$collection/post")
            val userId = operation.at("/parameters").first { it["name"].asText() == "userId" }
            assertEquals(operationId, operation.at("/operationId").asText())
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(userId.at("/schema/minimum").asDouble() > 0)
            assertEquals(
                "#/components/schemas/$request",
                operation.at("/requestBody/content/application~1json/schema/\$ref").asText(),
            )
            val wrapper = operation.at("/responses/201/content/application~1json/schema/\$ref").asText().substringAfterLast('/')
            assertEquals(
                "#/components/schemas/$response",
                document.at("/components/schemas/$wrapper/properties/data/\$ref").asText(),
                operationId,
            )
            assertTrue(operation.at("/responses/404/description").asText().contains("USER_NOT_FOUND"))
            assertTrue(operation.at("/responses/409/description").asText().contains("COMPANY_MEMBER_NOT_ACTIVE"))
            val requestSchema = document.at("/components/schemas/$request")
            assertEquals(listOf("HIDDEN", "VISIBLE"), requestSchema.at("/properties/visibility/enum").map { it.asText() }.sorted())
            assertFalse(requestSchema.at("/required").any { it.asText() == "visibility" }, request)
        }
        listOf(
            "AdminJobSummaryResponse",
            "AdminJobDetailResponse",
            "AdminBootcampSummaryResponse",
            "AdminBootcampDetailResponse",
        ).forEach { name ->
            val schema = document.at("/components/schemas/$name")
            assertEquals("boolean", schema.at("/properties/adminRegistered/type").asText(), name)
            assertTrue(schema.at("/required").any { it.asText() == "adminRegistered" }, name)
            assertTrue(schema.at("/properties/proxyRegisteredAt").isMissingNode, name)
        }
    }

    @Test
    fun `채용공고 분석 수정은 AI 서버 저장과 같은 분석 스키마를 받고 상세는 같은 스키마로 분석과 최신 여부를 싣는다`() {
        // given
        val document = openApiDocument()

        // when
        val operation = document.at("/paths/~1api~1v1~1admin~1jobs~1{jobId}~1analysis/put")
        val detail = document.at("/components/schemas/AdminJobDetailResponse/properties")

        // then
        assertEquals("replaceJobAnalysis", operation.at("/operationId").asText())
        assertEquals(listOf("jobId"), operation.at("/parameters").map { it["name"].asText() })
        assertEquals(
            "#/components/schemas/Analysis",
            operation.at("/requestBody/content/application~1json/schema/\$ref").asText(),
        )
        assertEquals(
            "#/components/schemas/Analysis",
            document.at("/components/schemas/AiJobAnalysisRequest/properties/analysis/\$ref").asText(),
        )
        assertEquals("#/components/schemas/Analysis", detail.at("/analysis/\$ref").asText())
        assertEquals("boolean", detail.at("/analysisCurrent/type").asText())
        assertTrue(detail.at("/analysisEditedAt").isObject)
        assertTrue(detail.at("/contentEditedAt").isObject)
        assertEquals(3, document.at("/components/schemas/Analysis/properties/tasks/maxItems").asInt())
        assertTrue(operation.at("/responses/404/description").asText().startsWith("JOB_NOT_FOUND"))
        assertTrue(
            document.at("/paths/~1api~1v1~1internal~1jobs~1{jobId}~1analysis/put/responses/409/description").asText()
                .contains("JOB_ANALYSIS_EDITED_BY_ADMIN"),
        )
    }

    @Test
    fun `관리자 OpenAPI는 독립된 정보와 인증 스키마를 제공하고 health를 제외한다`() {
        val response = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
        val document = objectMapper.readTree(response.contentAsString)

        assertEquals("Ogonggo Admin API", document.at("/info/title").asText())
        // 절대 URL이면 HTTP ALB 주소가 새어 나가 HTTPS Swagger UI에서 Try it out이 막힌다.
        assertEquals("/", document.at("/servers/0/url").asText())
        assertTrue(document.at("/components/securitySchemes/BearerAuth").isObject)
        assertFalse(document.at("/paths/~1health").isObject)
    }
}
