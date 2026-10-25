package com.amaizu.edgetree.edge.enums

import com.amaizu.edgetree.edge.exceptions.UnsupportedDeleteStrategyException

// How to treat the child's subtree when an edge is deleted, see DECISIONS.md for more details.
enum class DeleteStrategy {
    DETACH;

    companion object {
        // Case-insensitive
        fun from(value: String): DeleteStrategy =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw UnsupportedDeleteStrategyException(value, entries.map { it.name.lowercase() })
    }
}
