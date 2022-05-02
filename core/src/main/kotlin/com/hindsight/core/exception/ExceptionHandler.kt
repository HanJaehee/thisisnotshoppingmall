package com.hindsight.core.exception

import com.hindsight.core.response.ErrorResponse
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono


private val logger = KotlinLogging.logger {}

@Component
@Order(-2)
class ExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(errorAttributes, webProperties.resources, applicationContext) {
    init {
        super.setMessageReaders(serverCodecConfigurer.readers)
        super.setMessageWriters(serverCodecConfigurer.writers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> =
        RouterFunctions.route(RequestPredicates.all(), this::handleError)

    private fun handleError(request: ServerRequest): Mono<ServerResponse> {
        return when (val throwable = super.getError(request)) {
            is GlobalException -> {
                val globalException = throwable.globalMessage
                ServerResponse.status(globalException.status)
                    .bodyValue(
                        ErrorResponse(
                            name = globalException.name,
                            message = globalException.message
                        )
                    ).also {
                        logger.error("${globalException.name} ${globalException.message}", throwable)
                    }
            }
            else -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(
                    ErrorResponse(
                        name = throwable.javaClass.simpleName,
                        message = throwable.suppressedExceptions.toString()
                    )
                ).also {
                    logger.error(throwable.javaClass.simpleName, throwable)
                }

        }

    }
}