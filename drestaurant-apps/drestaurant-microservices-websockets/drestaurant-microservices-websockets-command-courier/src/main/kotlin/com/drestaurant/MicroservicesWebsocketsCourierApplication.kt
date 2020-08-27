package com.drestaurant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class MicroservicesWebsocketsCourierApplication

fun main(args: Array<String>) {
    runApplication<MicroservicesWebsocketsCourierApplication>(*args)
}
