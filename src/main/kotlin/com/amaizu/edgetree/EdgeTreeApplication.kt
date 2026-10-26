package com.amaizu.edgetree

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Swagger
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(info = Info(title = "Edge Tree API", version = "v1"))
@SpringBootApplication
class EdgeTreeApplication

fun main(args: Array<String>) {
    runApplication<EdgeTreeApplication>(*args)
}