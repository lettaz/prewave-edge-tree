package com.amaizu.edgetree.edge.exceptions

import com.amaizu.edgetree.edge.Edge

class CycleException(edge: Edge) : RuntimeException(
    "Adding ${edge.fromId} -> ${edge.toId} would create a cycle: ${edge.toId} is already an ancestor of ${edge.fromId}"
)
