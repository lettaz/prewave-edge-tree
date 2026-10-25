package com.amaizu.edgetree.edge

import com.amaizu.edgetree.edge.exceptions.CycleException
import com.amaizu.edgetree.edge.exceptions.EdgeAlreadyExistsException
import com.amaizu.edgetree.edge.exceptions.EdgeNotFoundException
import com.amaizu.edgetree.edge.exceptions.NodeAlreadyHasParentException
import com.amaizu.edgetree.edge.exceptions.SelfLinkException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.junit.jupiter.api.Assertions.assertSame


class EdgeServiceTests {
    private val repository = mock(EdgeRepository::class.java)
    private val service = EdgeService(repository)

    @Test
    fun `returns the edge when insertion succeeds`() {
        val edge = Edge(2, 4)
        given(repository.insertEdge(edge)).willReturn(1)

        val result = service.createEdge(edge)

        assertEquals(edge, result)
        verify(repository).insertEdge(edge)
    }

    @Test
    fun `rejects a self-link without touching the repository`() {
        val edge = Edge(20, 20)

        assertThrows(SelfLinkException::class.java) {
            service.createEdge(edge)
        }

        verifyNoInteractions(repository)
    }

    @Test
    fun `rejects a second parent without inserting`() {
        val edge = Edge(50, 30)
        given(repository.hasParent(30)).willReturn(true)

        assertThrows(NodeAlreadyHasParentException::class.java) {
            service.createEdge(edge)
        }

        verify(repository, never()).insertEdge(edge)
    }

    @Test
    fun `locks edge writes before checking the tree and inserting`() {
        val edge = Edge(2, 4)
        given(repository.insertEdge(edge)).willReturn(1)

        service.createEdge(edge)

        val order = inOrder(repository)
        order.verify(repository).lockEdgeWrites()
        order.verify(repository).hasParent(4)
        order.verify(repository).insertEdge(edge)
    }

    // Tree used below: 1→2, 1→3, 2→4, 2→5, 3→6
    @Test
    fun `rejects an edge that would create a cycle without inserting`() {
        val edge = Edge(4, 1)
        given(repository.hasChildren(1)).willReturn(true)
        given(repository.isAncestor(1, 4)).willReturn(true)

        assertThrows(CycleException::class.java) {
            service.createEdge(edge)
        }

        verify(repository, never()).insertEdge(edge)
    }

    @Test
    fun `allows attaching an existing root under a node outside its tree`() {
        val edge = Edge(10, 1)
        given(repository.hasChildren(1)).willReturn(true)
        given(repository.isAncestor(1, 10)).willReturn(false)
        given(repository.insertEdge(edge)).willReturn(1)

        assertEquals(edge, service.createEdge(edge))
    }

    @Test
    fun `skips the ancestor walk when the child has no children`() {
        val edge = Edge(6, 7)
        given(repository.hasChildren(7)).willReturn(false)
        given(repository.insertEdge(edge)).willReturn(1)

        service.createEdge(edge)

        verify(repository, never()).isAncestor(7, 6)
    }

    @Test
    fun `throws when the edge already exists`() {
        val edge = Edge(2, 4)
        given(repository.insertEdge(edge)).willReturn(0)

        assertThrows(EdgeAlreadyExistsException::class.java) {
            service.createEdge(edge)
        }

        verify(repository).insertEdge(edge)
    }

    @Test
    fun `throws for an unexpected inserted row count`() {
        val edge = Edge(2, 4)
        given(repository.insertEdge(edge)).willReturn(2)

        assertThrows(IllegalStateException::class.java) {
            service.createEdge(edge)
        }
    }

    @Test
    fun `propagates a repository failure unchanged`() {
        val edge = Edge(2, 4)
        val failure = RuntimeException("Repository failure")
        given(repository.insertEdge(edge)).willThrow(failure)

        val thrown = assertThrows(RuntimeException::class.java) {
            service.createEdge(edge)
        }

        assertSame(failure, thrown)
    }

    // --- deleteEdge ---

    @Test
    fun `deletes an existing edge`() {
        val edge = Edge(1, 2)
        given(repository.deleteEdge(edge)).willReturn(1)

        service.deleteEdge(edge)

        verify(repository).deleteEdge(edge)
    }

    @Test
    fun `throws when the edge to delete does not exist`() {
        val edge = Edge(1, 9)
        given(repository.deleteEdge(edge)).willReturn(0)

        assertThrows(EdgeNotFoundException::class.java) {
            service.deleteEdge(edge)
        }
    }
}