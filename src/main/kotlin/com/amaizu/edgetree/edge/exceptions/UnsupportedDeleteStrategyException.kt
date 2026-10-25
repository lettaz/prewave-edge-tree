package com.amaizu.edgetree.edge.exceptions

class UnsupportedDeleteStrategyException(value: String, supported: List<String>) : RuntimeException(
    "Unsupported strategy '$value'. Supported values: ${supported.joinToString()}."
)
