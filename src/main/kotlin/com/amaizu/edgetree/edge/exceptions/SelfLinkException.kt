package com.amaizu.edgetree.edge.exceptions

import com.amaizu.edgetree.edge.Edge

class SelfLinkException(edge: Edge) : RuntimeException(
    "A node cannot be linked to itself: ${edge.fromId} -> ${edge.toId}"
)
