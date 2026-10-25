package com.amaizu.edgetree.edge.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@RestControllerAdvice
class EdgeExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(EdgeAlreadyExistsException::class)
    fun handleEdgeAlreadyExists(exception: EdgeAlreadyExistsException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.message ?: "The edge already exists"
        ).apply {
            title = "Edge already exists"
        }
    }

    @ExceptionHandler(NodeAlreadyHasParentException::class)
    fun handleNodeAlreadyHasParent(exception: NodeAlreadyHasParentException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.message ?: "The node already has a parent"
        ).apply {
            title = "Node already has a parent"
        }
    }

    @ExceptionHandler(CycleException::class)
    fun handleCycle(exception: CycleException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.message ?: "The edge would create a cycle"
        ).apply {
            title = "Edge would create a cycle"
        }
    }

    @ExceptionHandler(SelfLinkException::class)
    fun handleSelfLink(exception: SelfLinkException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.message ?: "A node cannot be linked to itself"
        ).apply {
            title = "Self-link not allowed"
        }
    }
}
