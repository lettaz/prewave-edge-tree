package com.amaizu.edgetree.edge

import com.amaizu.edgetree.PostgresTestConfiguration
import com.amaizu.edgetree.generated.tables.references.EDGE
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException


@JooqTest
@AutoConfigureDataSourceInitialization
@Import(EdgeRepository::class, PostgresTestConfiguration::class)
class EdgeRepositoryTests @Autowired constructor(
    private val edgeRepository: EdgeRepository,
    private val dsl: DSLContext,
) {
    @Test
    fun `inserts an edge once and skips its duplicate`() {
        val edge = Edge(2, 4)
        val matchesEdge = EDGE.FROM_ID.eq(edge.fromId).and(EDGE.TO_ID.eq(edge.toId))

        assertEquals(1, edgeRepository.insertEdge(edge))
        assertEquals(0, edgeRepository.insertEdge(edge))
        assertEquals(1, dsl.fetchCount(EDGE, matchesEdge))
    }

    @Test
    fun `reports a parent once a node is the target of an edge`() {
        edgeRepository.insertEdge(Edge(20, 30))

        assertTrue(edgeRepository.hasParent(30))
    }

    @Test
    fun `reports no parent for a root or unknown node`() {
        edgeRepository.insertEdge(Edge(20, 30))

        assertFalse(edgeRepository.hasParent(20))
        assertFalse(edgeRepository.hasParent(99))
    }

    @Test
    fun `reports children only for a node that is the source of an edge`() {
        edgeRepository.insertEdge(Edge(1, 2))

        assertTrue(edgeRepository.hasChildren(1))
        assertFalse(edgeRepository.hasChildren(2))
        assertFalse(edgeRepository.hasChildren(99))
    }

    @Test
    fun `finds ancestors by walking up the tree`() {
        // 1→2, 1→3, 2→4
        edgeRepository.insertEdge(Edge(1, 2))
        edgeRepository.insertEdge(Edge(1, 3))
        edgeRepository.insertEdge(Edge(2, 4))

        assertTrue(edgeRepository.isAncestor(2, 4), "parent")
        assertTrue(edgeRepository.isAncestor(1, 4), "grandparent")
        assertFalse(edgeRepository.isAncestor(3, 4), "node in a sibling branch")
        assertFalse(edgeRepository.isAncestor(4, 1), "descendant, not ancestor")
        assertFalse(edgeRepository.isAncestor(99, 4), "unknown node")
        assertFalse(edgeRepository.isAncestor(1, 1), "a node is not its own ancestor")
    }

    @Test
    fun `database rejects a second parent for the same node`() {
        edgeRepository.insertEdge(Edge(20, 30))

        assertThrows(DataIntegrityViolationException::class.java) {
            edgeRepository.insertEdge(Edge(50, 30))
        }
    }

    @Test
    fun `database rejects a self-link`() {
        assertThrows(DataIntegrityViolationException::class.java) {
            edgeRepository.insertEdge(Edge(20, 20))
        }
    }

    @Test
    fun `deletes only the given edge and detaches the subtree below it`() {
        // 1→2, 2→4
        edgeRepository.insertEdge(Edge(1, 2))
        edgeRepository.insertEdge(Edge(2, 4))

        assertEquals(1, edgeRepository.deleteEdge(Edge(1, 2)))

        assertFalse(edgeRepository.hasParent(2), "2 is now a root")
        assertTrue(edgeRepository.hasChildren(2), "2 keeps its child 4")
        assertEquals(1, dsl.fetchCount(EDGE))
    }

    @Test
    fun `deleting a missing edge removes nothing`() {
        edgeRepository.insertEdge(Edge(1, 2))

        assertEquals(0, edgeRepository.deleteEdge(Edge(1, 9)))
        assertEquals(1, dsl.fetchCount(EDGE))
    }
}