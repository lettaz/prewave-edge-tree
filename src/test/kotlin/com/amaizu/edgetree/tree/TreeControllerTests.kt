package com.amaizu.edgetree.tree

import com.amaizu.edgetree.config.WebConfig
import com.amaizu.edgetree.ApiExceptionHandler
import com.amaizu.edgetree.tree.exceptions.NodeNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.willAnswer
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(TreeController::class)
@Import(WebConfig::class, ApiExceptionHandler::class)
class TreeControllerTests @Autowired constructor(
    private val mvc: MockMvc
) {

    @MockitoBean
    private lateinit var service: TreeService

    private fun anyConsumer(): (TreeNode) -> Unit {
        any<(TreeNode) -> Unit>()
        return {}
    }

    private fun stubTree(rootId: Int, maxDepth: Int?, nodes: List<TreeNode>) {
        willAnswer { invocation ->
            val onNode = invocation.getArgument<(TreeNode) -> Unit>(2)
            nodes.forEach(onNode)
        }.given(service).streamTree(eq(rootId), eq(maxDepth), anyConsumer())
    }

    @Test
    fun `returns the tree as a JSON array`() {
        stubTree(2, null, listOf(TreeNode(2, null, 0), TreeNode(4, 2, 1), TreeNode(5, 2, 1)))

        val pending = mvc.perform(get("/api/v1/trees/2"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mvc.perform(asyncDispatch(pending))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """[{"id":2,"parentId":null,"depth":0},{"id":4,"parentId":2,"depth":1},{"id":5,"parentId":2,"depth":1}]""",
                    JsonCompareMode.STRICT
                )
            )

        verify(service).requireNode(2)
    }

    @Test
    fun `passes maxDepth to the service`() {
        stubTree(1, 2, emptyList())

        val pending = mvc.perform(get("/api/v1/trees/1?maxDepth=2"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mvc.perform(asyncDispatch(pending))
            .andExpect(status().isOk)
            .andExpect(content().json("[]", JsonCompareMode.STRICT))

        verify(service).streamTree(eq(1), eq(2), anyConsumer())
    }

    @Test
    fun `returns 404 for an unknown node`() {
        willThrow(NodeNotFoundException(99)).given(service).requireNode(99)

        mvc.perform(get("/api/v1/trees/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Node not found"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/api/v1/trees/0",
            "/api/v1/trees/-1",
            "/api/v1/trees/abc",
            "/api/v1/trees/1?maxDepth=-1",
            "/api/v1/trees/1?maxDepth=abc",
        ]
    )
    fun `rejects invalid input before calling the service`(url: String) {
        mvc.perform(get(url))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(service)
    }
}
