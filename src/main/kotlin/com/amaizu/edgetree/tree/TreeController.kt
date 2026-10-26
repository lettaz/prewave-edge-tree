package com.amaizu.edgetree.tree

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import tools.jackson.databind.json.JsonMapper

// Swagger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Trees", description = "Read a subtree, streamed breadth-first")
@RestController
@RequestMapping(path = ["/trees"], version = "v1")
class TreeController(
    private val service: TreeService,
    private val jsonMapper: JsonMapper,
) {

    // subtree as a JSON array stream, so the full tree is never in memory.
    @Operation(summary = "Get the subtree rooted at a node, streamed breadth-first")
    @ApiResponse(
        responseCode = "200",
        description = "Flat list of nodes, level by level. The requested node has parentId null and depth 0.",
        content = [Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = TreeNode::class))
        )]
    )
    @ApiResponse(responseCode = "400", description = "nodeId not positive, or maxDepth negative")
    @ApiResponse(responseCode = "404", description = "Node appears in no edge")

    @GetMapping("/{nodeId}")
    fun getTree(
        @PathVariable @Positive nodeId: Int,
        @RequestParam(required = false) @PositiveOrZero maxDepth: Int?,
    ): ResponseEntity<StreamingResponseBody> {
        service.requireNode(nodeId)

        val body = StreamingResponseBody { output ->
            jsonMapper.createGenerator(output).use { json ->
                json.writeStartArray()
                service.streamTree(nodeId, maxDepth) { node -> json.writePOJO(node) }
                json.writeEndArray()
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    }
}
