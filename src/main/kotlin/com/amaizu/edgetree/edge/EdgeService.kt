package com.amaizu.edgetree.edge

import com.amaizu.edgetree.edge.enums.DeleteStrategy
import com.amaizu.edgetree.edge.exceptions.CycleException
import com.amaizu.edgetree.edge.exceptions.EdgeAlreadyExistsException
import com.amaizu.edgetree.edge.exceptions.EdgeNotFoundException
import com.amaizu.edgetree.edge.exceptions.NodeAlreadyHasParentException
import com.amaizu.edgetree.edge.exceptions.SelfLinkException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EdgeService(
    private val repository: EdgeRepository
) {
    private val log = LoggerFactory.getLogger(EdgeService::class.java)

    @Transactional
    fun createEdge(edge: Edge): Edge {
        if (edge.fromId == edge.toId) throw SelfLinkException(edge)

        repository.lockEdgeWrites()
        if (repository.hasParent(edge.toId)) throw NodeAlreadyHasParentException(edge)

        // Goal is to prevent cycle creation...possibly expensive due to the tree depth but trade-offs seen in DECISIONS.md
        if (repository.hasChildren(edge.toId) && repository.isAncestor(edge.toId, edge.fromId)) {
            throw CycleException(edge)
        }

        val insertedRows = repository.insertEdge(edge)

        return when (insertedRows) {
            1 -> edge.also { log.info("Created edge {} -> {}", edge.fromId, edge.toId) }
            0 -> throw EdgeAlreadyExistsException(edge)
            else -> error("Unexpected number of rows inserted: $insertedRows")
        }
    }

    fun deleteEdge(edge: Edge, strategy: DeleteStrategy = DeleteStrategy.DETACH) {
        val deletedRows = when (strategy) {
            DeleteStrategy.DETACH -> repository.deleteEdge(edge)
        }

        if (deletedRows == 0) throw EdgeNotFoundException(edge)

        log.info("Deleted edge {} -> {} (strategy={})", edge.fromId, edge.toId, strategy)
    }
}