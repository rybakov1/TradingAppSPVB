package rybakov.vladislav.tradingappspvb.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import rybakov.vladislav.tradingappspvb.exception.ErrorResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ApiErrorResponses {

    private static final String JSON = "application/json";

    private ApiErrorResponses() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            useReturnTypeSchema = true
    )
    public @interface Ok {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "400",
            description = "The request body is malformed or fails validation",
            content = @Content(
                    mediaType = JSON,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    public @interface BadRequest {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "404",
            description = "The requested resource does not exist",
            content = @Content(
                    mediaType = JSON,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    public @interface NotFound {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "409",
            description = "The current resource state conflicts with the operation",
            content = @Content(
                    mediaType = JSON,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    public @interface Conflict {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "422",
            description = "The request violates a trading business rule",
            content = @Content(
                    mediaType = JSON,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    public @interface Unprocessable {
    }
}
