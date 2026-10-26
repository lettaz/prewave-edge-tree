package com.amaizu.edgetree.tree.exceptions

class NodeNotFoundException(nodeId: Int) : RuntimeException(
    "Node not found: $nodeId"
)
