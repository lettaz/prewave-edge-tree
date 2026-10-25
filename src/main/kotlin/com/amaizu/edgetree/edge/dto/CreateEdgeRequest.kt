package com.amaizu.edgetree.edge.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

// validate befre creating an edge
data class CreateEdgeRequest(
    @field:NotNull(message = "fromId is required")
    @field:Positive(message = "fromId must be greater than zero")
    val fromId: Int? = null,

    @field:NotNull(message = "toId is required")
    @field:Positive(message = "toId must be greater than zero")
    val toId: Int? = null
)
