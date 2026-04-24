package io.resrv.adapter.in.web.error;

import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.security.ForbiddenOperationException;
import io.resrv.application.tenant.TenantNotFoundException;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.customer.CustomerNotFoundException;
import io.resrv.domain.reservation.ReservationCancellationClosedException;
import io.resrv.domain.reservation.ReservationHoldExpiredException;
import io.resrv.domain.reservation.ReservationInvalidStateException;
import io.resrv.domain.reservation.ReservationNotFoundException;
import io.resrv.domain.reservation.SlotUnavailableException;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceSlugAlreadyExistsException;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.DateTimeException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(
            final MethodArgumentNotValidException exception, final HttpServletRequest request) {
        final var problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        final var fieldErrors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        new FieldError(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()))
                        .toList();

        problemDetail.setProperty("fieldErrors", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler({
        SlugAlreadyExistsException.class,
        ResourceSlugAlreadyExistsException.class,
        CustomerEmailAlreadyExistsException.class,
        SlotUnavailableException.class,
        ReservationHoldExpiredException.class,
        ReservationInvalidStateException.class,
        ReservationCancellationClosedException.class
    })
    ProblemDetail handleConflict(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
        ResourceNotFoundException.class,
        TenantNotFoundException.class,
        CustomerNotFoundException.class,
        ReservationNotFoundException.class
    })
    ProblemDetail handleNotFound(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    ProblemDetail handleForbidden(
            final ForbiddenOperationException exception, final HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(
            final AuthenticationFailedException exception, final HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", request);
    }

    @ExceptionHandler(DateTimeException.class)
    ProblemDetail handleDateTimeException(final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid date or time parameter", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMessageNotReadable(final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON or missing request body", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(final Exception exception, final HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            final var problemDetail = ProblemDetail.forStatus(errorResponse.getStatusCode());
            problemDetail.setInstance(URI.create(request.getRequestURI()));
            return problemDetail;
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private static ProblemDetail problem(
            final HttpStatus status, final String detail, final HttpServletRequest request) {
        final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    record FieldError(String field, String message) {

        FieldError(final String field, @Nullable final String message) {
            this.field = field;
            this.message = Objects.requireNonNullElse(message, "Invalid value");
        }
    }
}
