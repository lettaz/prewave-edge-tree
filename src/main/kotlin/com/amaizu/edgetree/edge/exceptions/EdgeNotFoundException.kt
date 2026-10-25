package com.amaizu.edgetree.edge.exceptions

import com.amaizu.edgetree.edge.Edge

class EdgeNotFoundException(edge: Edge) : RuntimeException(
    "Edge not found: ${edge.fromId} -> ${edge.toId}"
)
