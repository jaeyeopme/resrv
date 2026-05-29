package io.resrv.timeslot.adapter.in.web.error;

import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.discovery.PublicDiscoveryNotFoundException;
import io.resrv.timeslot.application.reservation.ReservationAccessDeniedException;
import io.resrv.timeslot.application.reservation.ReservationNotFoundException;
import io.resrv.timeslot.application.reservation.SlotUnavailableException;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.ResourceSlugAlreadyExistsException;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.domain.reservation.ReservationHoldExpiredException;
import io.resrv.timeslot.domain.reservation.ReservationInvalidStateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.DateTimeException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "io.resrv.timeslot.adapter.in.web")
class TimeslotExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(
            final MethodArgumentNotValidException exception, final HttpServletRequest request) {
        final var problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(
                "fieldErrors",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        new FieldError(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()))
                        .toList());
        return problemDetail;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleHandlerMethodValidation(
            final HandlerMethodValidationException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(
            final ConstraintViolationException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", request);
    }

    @ExceptionHandler(ResourceSlugAlreadyExistsException.class)
    ProblemDetail handleConflict(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
        ReservationAccessDeniedException.class,
    })
    ProblemDetail handleForbidden(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({
        BusinessNotAvailableException.class,
        PublicDiscoveryNotFoundException.class,
        ReservationNotFoundException.class,
    })
    ProblemDetail handleNotFound(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotAvailableException.class)
    ProblemDetail handleResourceNotFound(
            final ResourceNotAvailableException exception, final HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler({
        SlotUnavailableException.class,
        ReservationHoldExpiredException.class,
        ReservationInvalidStateException.class,
        BookingSettingsRequiredException.class,
    })
    ProblemDetail handleUnprocessable(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class})
    ProblemDetail handleBadRequest(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", request);
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

        FieldError(final String field, final String message) {
            this.field = field;
            this.message = Objects.requireNonNullElse(message, "Invalid value");
        }
    }
}
