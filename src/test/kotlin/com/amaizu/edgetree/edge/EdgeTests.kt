package com.amaizu.edgetree.edge

import com.amaizu.edgetree.PostgresTestConfiguration
import com.amaizu.edgetree.edge.exceptions.CycleException
import com.amaizu.edgetree.generated.tables.references.EDGE
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@SpringBootTest
@Import(PostgresTestConfiguration::class)
class EdgeTests @Autowired constructor(
    private val service: EdgeService,
    private val dsl: DSLContext,
) {

    @BeforeEach
    fun clearEdges() {
        dsl.deleteFrom(EDGE).execute()
    }

    @Test
    fun `concurrent creates cannot form a cycle together`() {
        service.createEdge(Edge(20, 30))
        service.createEdge(Edge(30, 40))

        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val results = listOf(Edge(40, 60), Edge(60, 20)).map { edge ->
            pool.submit<Result<Edge>> {
                start.await()
                runCatching { service.createEdge(edge) }
            }
        }
        start.countDown()
        val outcomes = results.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, outcomes.count { it.isSuccess }, "exactly one request succeeds")
        assertTrue(outcomes.single { it.isFailure }.exceptionOrNull() is CycleException)
        assertEquals(3, dsl.fetchCount(EDGE))
    }
}
