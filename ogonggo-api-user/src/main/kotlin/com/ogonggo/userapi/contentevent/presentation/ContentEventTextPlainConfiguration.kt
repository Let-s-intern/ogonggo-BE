package com.ogonggo.userapi.contentevent.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.userapi.contentevent.presentation.request.CreateContentEventsRequest
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.lang.reflect.Type

/**
 * 콘텐츠 이벤트 요청만 text/plain 본문을 JSON으로 읽는다.
 *
 * 브라우저는 페이지를 떠날 때 navigator.sendBeacon으로 보내는데, 다른 오리진으로 보낼 수 있는 Content-Type이
 * text/plain 같은 단순 타입뿐이다. 모든 요청이 text/plain JSON을 받게 하지 않으려고 이 요청 타입에만 연다.
 */
@Configuration
class ContentEventTextPlainConfiguration(
    private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(0, TextPlainContentEventsConverter(objectMapper))
    }

    private class TextPlainContentEventsConverter(objectMapper: ObjectMapper) : MappingJackson2HttpMessageConverter(objectMapper) {

        init {
            supportedMediaTypes = listOf(MediaType.TEXT_PLAIN)
        }

        override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean =
            clazz == CreateContentEventsRequest::class.java && super.canRead(clazz, mediaType)

        override fun canRead(type: Type, contextClass: Class<*>?, mediaType: MediaType?): Boolean =
            type == CreateContentEventsRequest::class.java && super.canRead(type, contextClass, mediaType)

        override fun canWrite(clazz: Class<*>, mediaType: MediaType?): Boolean = false

        override fun canWrite(type: Type?, clazz: Class<*>, mediaType: MediaType?): Boolean = false
    }
}
