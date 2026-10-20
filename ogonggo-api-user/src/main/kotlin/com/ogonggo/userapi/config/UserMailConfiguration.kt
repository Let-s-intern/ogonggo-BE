package com.ogonggo.userapi.config

import com.ogonggo.userapi.mail.OgonggoMailProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OgonggoMailProperties::class)
class UserMailConfiguration
