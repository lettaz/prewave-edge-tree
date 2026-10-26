package com.amaizu.edgetree.tree

import com.amaizu.edgetree.tree.exceptions.NodeNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TreeService(
    private val repository: TreeRepository
) {
    private val log = LoggerFactory.getLogger(TreeService::class.java)

    fun requireNode(nodeId: Int) {
        if (!repository.nodeExists(nodeId)) throw NodeNotFoundException(nodeId)
    }

    @Transactional(readOnly = true)
    fun streamTree(rootId: Int, maxDepth: Int?, onNode: (TreeNode) -> Unit) {
        val startedAt = System.nanoTime()
        var count = 0

        repository.streamTree(rootId, maxDepth) { node ->
            count++
            onNode(node)
        }

        log.debug(
            "Streamed {} nodes from root {} (maxDepth={}) in {} ms",
            count, rootId, maxDepth, (System.nanoTime() - startedAt) / 1_000_000
        )
    }
}
