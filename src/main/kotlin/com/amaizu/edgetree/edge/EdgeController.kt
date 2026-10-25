package com.amaizu.edgetree.edge

import com.amaizu.edgetree.edge.dto.CreateEdgeRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/edges"], version = "1")
class EdgeController(
    private val service: EdgeService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEdge(@Valid @RequestBody request: CreateEdgeRequest): Edge {
        val edge = Edge(
            fromId = requireNotNull(request.fromId),
            toId = requireNotNull(request.toId)
        )

        return service.createEdge(edge)
    }
}
