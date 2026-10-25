package com.amaizu.edgetree.edge.exceptions

import com.amaizu.edgetree.edge.Edge

class NodeAlreadyHasParentException(edge: Edge) : RuntimeException(
    "Node ${edge.toId} already has a parent; cannot add ${edge.fromId} -> ${edge.toId}"
)
