package com.amaizu.edgetree.edge.dto

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class DeleteEdgeRequest(
    @field:Parameter(required = true)
    @field:NotNull(message = "fromId is required")
    @field:Positive(message = "fromId must be greater than zero")
    val fromId: Int? = null,

    @field:Parameter(required = true)
    @field:NotNull(message = "toId is required")
    @field:Positive(message = "toId must be greater than zero")
    val toId: Int? = null,

    @field:Parameter(schema = Schema(defaultValue = "detach", allowableValues = ["detach"]))
    val strategy: String = "detach"

)
