package com.amaizu.edgetree.tree

import com.amaizu.edgetree.generated.tables.references.EDGE
import org.jooq.DSLContext
import org.jooq.impl.DSL.castNull
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.noCondition
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.value
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository

@Repository
class TreeRepository(private val dsl: DSLContext) {

    fun nodeExists(nodeId: Int): Boolean {
        return dsl.fetchExists(EDGE, EDGE.FROM_ID.eq(nodeId).or(EDGE.TO_ID.eq(nodeId)))
    }

    // Streams the subtree row by row (in a transaction), intentionally no ORDER BY, see DECISIONS.md.
    fun streamTree(rootId: Int, maxDepth: Int?, onNode: (TreeNode) -> Unit) {
        val tree = name("tree")
        val id = field(name("tree", "id"), SQLDataType.INTEGER)
        val parentId = field(name("tree", "parent_id"), SQLDataType.INTEGER)
        val depth = field(name("tree", "depth"), SQLDataType.INTEGER)

        val walkDown = tree.fields("id", "parent_id", "depth").`as`(

            select(value(rootId).coerce(EDGE.TO_ID), castNull(EDGE.FROM_ID), inline(0).coerce(depth))
                .unionAll(
                    // Each step - the children of the nodes found in the previous step.
                    select(EDGE.TO_ID, EDGE.FROM_ID, depth.plus(1))
                        .from(EDGE)
                        .join(table(tree)).on(EDGE.FROM_ID.eq(id))
                        .where(if (maxDepth == null) noCondition() else depth.lt(maxDepth))
                )
        )

        dsl.withRecursive(walkDown)
            .select(id, parentId, depth)
            .from(walkDown)
            .fetchSize(FETCH_SIZE)
            .fetchLazy()
            .use { cursor ->
                for (row in cursor) {
                    onNode(TreeNode(row.value1(), row.value2(), row.value3()))
                }
            }
    }

    private companion object {
        // Rows fetched from PostgreSQL per round trip while streaming.
        const val FETCH_SIZE = 1_000
    }
}
