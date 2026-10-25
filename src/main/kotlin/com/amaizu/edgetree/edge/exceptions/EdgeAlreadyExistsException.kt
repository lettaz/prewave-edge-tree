package com.amaizu.edgetree.edge.exceptions

import com.amaizu.edgetree.edge.Edge

class EdgeAlreadyExistsException(edge: Edge) : RuntimeException(
    "Edge already exists: ${edge.fromId} -> ${edge.toId}"
)