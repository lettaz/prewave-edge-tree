package com.amaizu.edgetree.tree

import com.amaizu.edgetree.PostgresTestConfiguration
import com.amaizu.edgetree.generated.tables.references.EDGE
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration::class)
class TreeTests @Autowired constructor(
    private val mvc: MockMvc,
    private val dsl: DSLContext,
) {

    @BeforeEach
    fun clearEdges() {
        dsl.deleteFrom(EDGE).execute()
    }

    @Test
    fun `creates the brief's tree through the API and fetches it breadth-first`() {
        listOf(1 to 2, 1 to 3, 2 to 4, 2 to 5, 3 to 6).forEach { (from, to) ->
            mvc.perform(
                post("/api/v1/edges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"fromId":$from,"toId":$to}""")
            ).andExpect(status().isCreated)
        }

        val pending = mvc.perform(get("/api/v1/trees/1"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mvc.perform(asyncDispatch(pending))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[*].depth", contains(0, 1, 1, 2, 2, 2)))
            .andExpect(jsonPath("$[*].id", containsInAnyOrder(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun `returns 404 for a node that is in no edge`() {
        mvc.perform(get("/api/v1/trees/99"))
            .andExpect(status().isNotFound)
    }
}
