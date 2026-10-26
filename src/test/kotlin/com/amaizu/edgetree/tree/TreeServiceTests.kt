package com.amaizu.edgetree.tree

import com.amaizu.edgetree.tree.exceptions.NodeNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.mock

class TreeServiceTests {
    private val repository = mock(TreeRepository::class.java)
    private val service = TreeService(repository)

    // Mockito's any() returns null, which Kotlin rejects for a non-null lambda parameter.
    private fun anyConsumer(): (TreeNode) -> Unit {
        any<(TreeNode) -> Unit>()
        return {}
    }

    @Test
    fun `requireNode passes for an existing node`() {
        given(repository.nodeExists(4)).willReturn(true)

        service.requireNode(4)
    }

    @Test
    fun `requireNode throws for a node that is in no edge`() {
        given(repository.nodeExists(99)).willReturn(false)

        assertThrows(NodeNotFoundException::class.java) {
            service.requireNode(99)
        }
    }

    @Test
    fun `streamTree passes root and maxDepth on and forwards every node`() {
        val nodes = listOf(TreeNode(1, null, 0), TreeNode(2, 1, 1), TreeNode(3, 1, 1))
        willAnswer { invocation ->
            val onNode = invocation.getArgument<(TreeNode) -> Unit>(2)
            nodes.forEach(onNode)
        }.given(repository).streamTree(eq(1), eq(2), anyConsumer())

        val received = mutableListOf<TreeNode>()
        service.streamTree(1, 2) { received.add(it) }

        assertEquals(nodes, received)
    }
}
