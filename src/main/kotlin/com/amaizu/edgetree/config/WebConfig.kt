package com.amaizu.edgetree.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            "/api/{version}",
            HandlerTypePredicate.forAnnotation(RestController::class.java)
        )
    }

    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer.usePathSegment(1)
    }
}
