package com.amaizu.edgetree.edge

import com.amaizu.edgetree.generated.tables.references.EDGE
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository

@Repository
class EdgeRepository(private val dsl: DSLContext) {

    // considering race condition, we need to lock the edge writes
    fun lockEdgeWrites() {
        dsl.fetch("SELECT 1 FROM pg_advisory_xact_lock(?)", EDGE_WRITE_LOCK)
    }

    fun insertEdge(edge: Edge): Int {
        return dsl
            .insertInto(EDGE, EDGE.FROM_ID, EDGE.TO_ID)
            .values(edge.fromId, edge.toId)
            .onConflict(EDGE.FROM_ID, EDGE.TO_ID)
            .doNothing()
            .execute()
    }


    fun deleteEdge(edge: Edge): Int {
        return dsl
            .deleteFrom(EDGE)
            .where(EDGE.FROM_ID.eq(edge.fromId))
            .and(EDGE.TO_ID.eq(edge.toId))
            .execute()
    }

    fun hasParent(childId: Int): Boolean {
        return dsl.fetchExists(EDGE, EDGE.TO_ID.eq(childId))
    }

    fun hasChildren(parentId: Int): Boolean {
        return dsl.fetchExists(EDGE, EDGE.FROM_ID.eq(parentId))
    }

    fun isAncestor(ancestorId: Int, nodeId: Int): Boolean {
        val ancestors = name("ancestors")
        val ancestorNode = field(name("ancestors", "node_id"), SQLDataType.INTEGER)

        val walkUp = ancestors.fields("node_id").`as`(
            select(EDGE.FROM_ID).from(EDGE).where(EDGE.TO_ID.eq(nodeId))
                .union(
                    select(EDGE.FROM_ID).from(EDGE)
                        .join(table(ancestors)).on(EDGE.TO_ID.eq(ancestorNode))
                )
        )

        return dsl.fetchExists(
            dsl.withRecursive(walkUp)
                .selectOne()
                .from(walkUp)
                .where(ancestorNode.eq(ancestorId))
        )
    }

    private companion object {
        const val EDGE_WRITE_LOCK = 7_210_001L
    }
}