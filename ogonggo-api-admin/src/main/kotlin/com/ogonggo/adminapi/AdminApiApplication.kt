package com.ogonggo.adminapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.ogonggo"])
class AdminApiApplication

fun main(args: Array<String>) {
    runApplication<AdminApiApplication>(*args)
}
