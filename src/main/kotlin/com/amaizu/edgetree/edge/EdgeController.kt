package com.amaizu.edgetree.edge

import com.amaizu.edgetree.edge.dto.CreateEdgeRequest
import com.amaizu.edgetree.edge.dto.DeleteEdgeRequest
import com.amaizu.edgetree.edge.enums.DeleteStrategy
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

// Swagger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject


@Tag(name = "Edges", description = "Create and delete edges")
@RestController
@RequestMapping(path = ["/edges"], version = "v1")
class EdgeController(
    private val service: EdgeService
) {
    @Operation(summary = "Create an edge from a parent to a child")
    @ApiResponse(responseCode = "201", description = "Edge created")
    @ApiResponse(responseCode = "400", description = "Invalid ids, or a node linked to itself")
    @ApiResponse(responseCode = "409", description = "Edge already exists, the child already has a parent, or it would create a cycle")

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEdge(@Valid @RequestBody request: CreateEdgeRequest): Edge {
        val edge = Edge(
            fromId = requireNotNull(request.fromId),
            toId = requireNotNull(request.toId)
        )

        return service.createEdge(edge)
    }

    @Operation(summary = "Delete an edge, the child keeps its own subtree")
    @ApiResponse(responseCode = "204", description = "Edge deleted")
    @ApiResponse(responseCode = "400", description = "Invalid ids, or an empty or unsupported strategy")
    @ApiResponse(responseCode = "404", description = "Edge does not exist")

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEdge(@Valid @ParameterObject request: DeleteEdgeRequest) {
        val edge = Edge(
            fromId = requireNotNull(request.fromId),
            toId = requireNotNull(request.toId)
        )

        service.deleteEdge(edge, DeleteStrategy.from(request.strategy))
    }
}