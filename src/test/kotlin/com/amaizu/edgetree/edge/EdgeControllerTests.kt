package com.amaizu.edgetree.edge

import com.amaizu.edgetree.config.WebConfig
import com.amaizu.edgetree.ApiExceptionHandler
import com.amaizu.edgetree.edge.exceptions.CycleException
import com.amaizu.edgetree.edge.exceptions.EdgeAlreadyExistsException
import com.amaizu.edgetree.edge.exceptions.EdgeNotFoundException
import com.amaizu.edgetree.edge.exceptions.NodeAlreadyHasParentException
import com.amaizu.edgetree.edge.exceptions.SelfLinkException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(EdgeController::class)
@Import(WebConfig::class, ApiExceptionHandler::class)
class EdgeControllerTests @Autowired constructor(
    private val mvc: MockMvc
) {

    @MockitoBean
    private lateinit var service: EdgeService

    @Test
    fun `creates an edge and returns 201`() {
        val edge = Edge(2, 4)
        given(service.createEdge(edge)).willReturn(edge)

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":2,"toId":4}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fromId").value(2))
            .andExpect(jsonPath("$.toId").value(4))

        verify(service).createEdge(edge)
    }

    @Test
    fun `accepts positive integer IDs at the supported upper bound`() {
        val edge = Edge(Int.MAX_VALUE, Int.MAX_VALUE - 1)
        given(service.createEdge(edge)).willReturn(edge)

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":2147483647,"toId":2147483646}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fromId").value(Int.MAX_VALUE))
            .andExpect(jsonPath("$.toId").value(Int.MAX_VALUE - 1))

        verify(service).createEdge(edge)
    }

    @Test
    fun `returns 409 when the edge already exists`() {
        val edge = Edge(2, 4)
        given(service.createEdge(edge)).willThrow(EdgeAlreadyExistsException(edge))

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":2,"toId":4}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Edge already exists"))

        verify(service).createEdge(edge)
    }

    @Test
    fun `returns 409 when the edge would create a cycle`() {
        val edge = Edge(4, 1)
        given(service.createEdge(edge)).willThrow(CycleException(edge))

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":4,"toId":1}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Edge would create a cycle"))

        verify(service).createEdge(edge)
    }

    @Test
    fun `returns 409 when the child already has a parent`() {
        val edge = Edge(50, 30)
        given(service.createEdge(edge)).willThrow(NodeAlreadyHasParentException(edge))

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":50,"toId":30}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Node already has a parent"))

        verify(service).createEdge(edge)
    }

    @Test
    fun `returns 400 when the edge links a node to itself`() {
        val edge = Edge(20, 20)
        given(service.createEdge(edge)).willThrow(SelfLinkException(edge))

        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":20,"toId":20}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Self-link not allowed"))

        verify(service).createEdge(edge)
    }

    @Test
    fun `rejects an unsupported API version before calling the service`() {
        mvc.perform(
            post("/api/v2/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":2,"toId":4}""")
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(service)
    }

    @Test
    fun `does not expose the unversioned route`() {
        mvc.perform(
            post("/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fromId":2,"toId":4}""")
        )
            .andExpect(status().is4xxClientError)

        verifyNoInteractions(service)
    }

    // Just to indicate, yes its long parameter list but it was written by me.
    @ParameterizedTest
    @ValueSource(
        strings = [
            "{}",
            """{"fromId":2}""",
            """{"toId":4}""",
            """{"fromId":null,"toId":4}""",
            """{"fromId":2,"toId":null}""",
            """{"fromId":0,"toId":4}""",
            """{"fromId":2,"toId":0}""",
            """{"fromId":-2,"toId":4}""",
            """{"fromId":2,"toId":-4}""",
            """{"fromId":2.5,"toId":4}""",
            """{"fromId":2,"toId":4.5}""",
            """{"fromId":2147483648,"toId":4}""",
            """{"fromId":2,"toId":2147483648}""",
            """{"fromId":"two","toId":4}""",
            """{"fromId":"2","toId":4}""",
            """{"fromId":true,"toId":4}""",
            """{"fromId":2,"toId":false}""",
            "null",
            "{"
        ]
    )
    fun `rejects invalid input before calling the service`(json: String) {
        mvc.perform(
            post("/api/v1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(service)
    }

    // --- DELETE EDGE ---

    @Test
    fun `deletes an edge and returns 204`() {
        mvc.perform(delete("/api/v1/edges?fromId=1&toId=2"))
            .andExpect(status().isNoContent)

        verify(service).deleteEdge(Edge(1, 2))
    }

    @Test
    fun `accepts the detach strategy explicitly`() {
        mvc.perform(delete("/api/v1/edges?fromId=1&toId=2&strategy=detach"))
            .andExpect(status().isNoContent)

        verify(service).deleteEdge(Edge(1, 2))
    }

    @Test
    fun `returns 404 when the edge does not exist`() {
        val edge = Edge(1, 9)
        willThrow(EdgeNotFoundException(edge)).given(service).deleteEdge(edge)

        mvc.perform(delete("/api/v1/edges?fromId=1&toId=9"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Edge not found"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "?fromId=1",
            "?toId=2",
            "?fromId=0&toId=2",
            "?fromId=1&toId=-2",
            "?fromId=one&toId=2",
            "?fromId=1&toId=2.5",
            "?fromId=1&toId=2&strategy=cascade",
            "?fromId=1&toId=2&strategy="
        ]
    )
    fun `rejects an invalid delete request before calling the service`(query: String) {
        mvc.perform(delete("/api/v1/edges$query"))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(service)
    }
}
