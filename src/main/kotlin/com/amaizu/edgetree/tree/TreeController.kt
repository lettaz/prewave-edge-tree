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

@RestController
@RequestMapping(path = ["/trees"], version = "1")
class TreeController(
    private val service: TreeService,
    private val jsonMapper: JsonMapper,
) {

    // subtree as a JSON array stream, so the full tree is never in memory.
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
