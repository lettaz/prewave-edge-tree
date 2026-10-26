package com.amaizu.edgetree.tree

import com.amaizu.edgetree.PostgresTestConfiguration
import com.amaizu.edgetree.generated.tables.references.EDGE
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization
import org.springframework.context.annotation.Import

@JooqTest
@AutoConfigureDataSourceInitialization
@Import(TreeRepository::class, PostgresTestConfiguration::class)
class TreeRepositoryTests @Autowired constructor(
    private val treeRepository: TreeRepository,
    private val dsl: DSLContext,
) {

    // The brief's example tree: 1→2, 1→3, 2→4, 2→5, 3→6
    @BeforeEach
    fun insertExampleTree() {
        listOf(1 to 2, 1 to 3, 2 to 4, 2 to 5, 3 to 6).forEach { (from, to) ->
            dsl.insertInto(EDGE, EDGE.FROM_ID, EDGE.TO_ID).values(from, to).execute()
        }
    }

    private fun collect(rootId: Int, maxDepth: Int? = null): List<TreeNode> =
        mutableListOf<TreeNode>().also { nodes ->
            treeRepository.streamTree(rootId, maxDepth) { nodes.add(it) }
        }

    @Test
    fun `returns the whole tree from the root with parent and depth`() {
        assertEquals(
            setOf(
                TreeNode(1, null, 0),
                TreeNode(2, 1, 1), TreeNode(3, 1, 1),
                TreeNode(4, 2, 2), TreeNode(5, 2, 2), TreeNode(6, 3, 2),
            ),
            collect(1).toSet()
        )
    }

    @Test
    fun `returns only the subtree below an inner node, with that node as root`() {
        assertEquals(
            setOf(TreeNode(2, null, 0), TreeNode(4, 2, 1), TreeNode(5, 2, 1)),
            collect(2).toSet()
        )
    }

    @Test
    fun `returns just the node itself for a leaf`() {
        assertEquals(listOf(TreeNode(4, null, 0)), collect(4))
    }

    @Test
    fun `stops at maxDepth`() {
        assertEquals(
            setOf(TreeNode(1, null, 0), TreeNode(2, 1, 1), TreeNode(3, 1, 1)),
            collect(1, maxDepth = 1).toSet()
        )
        assertEquals(listOf(TreeNode(1, null, 0)), collect(1, maxDepth = 0))
    }

    @Test
    fun `returns nodes in breadth-first order`() {
        dsl.execute("INSERT INTO edge SELECT (random() * (n - 8))::int + 7, n FROM generate_series(8, 5000) n")

        val depths = collect(7).map { it.depth }

        assertEquals(4994, depths.size)
        assertTrue(depths.zipWithNext().all { (a, b) -> a <= b }, "depth must never decrease")
    }

    @Test
    fun `handles a tree 1,500 levels deep`() {
        dsl.execute("INSERT INTO edge SELECT n - 1, n FROM generate_series(10002, 11500) n")
        dsl.execute("INSERT INTO edge SELECT n, n + 10000 FROM generate_series(10001, 11500) n")

        val nodes = collect(10001)
        val depths = nodes.map { it.depth }

        assertEquals(3000, nodes.size, "1,500 spine nodes + 1,500 leaves")
        assertEquals(1500, depths.max(), "the leaf under the deepest spine node")
        assertTrue(depths.zipWithNext().all { (a, b) -> a <= b }, "depth must never decrease")
    }

    @Test
    fun `knows whether a node exists`() {
        assertTrue(treeRepository.nodeExists(1), "root: appears only as a parent")
        assertTrue(treeRepository.nodeExists(4), "leaf: appears only as a child")
        assertFalse(treeRepository.nodeExists(99), "in no edge")
    }
}
