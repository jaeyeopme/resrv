package io.resrv.platform.adapter.in.web.error;

import io.resrv.platform.application.auth.AuthenticationFailedException;
import io.resrv.platform.application.auth.PasswordResetRequiredException;
import io.resrv.platform.application.auth.PasswordResetTokenInvalidException;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
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

@RestControllerAdvice(basePackages = "io.resrv.platform.adapter.in.web")
class PlatformExceptionHandler {

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
        AccountEmailAlreadyExistsException.class,
        BusinessSlugAlreadyExistsException.class
    })
    ProblemDetail handleConflict(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(
            final AuthenticationFailedException exception, final HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(PasswordResetRequiredException.class)
    ProblemDetail handlePasswordResetRequired(
            final PasswordResetRequiredException exception, final HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(PasswordResetTokenInvalidException.class)
    ProblemDetail handlePasswordResetTokenInvalid(
            final PasswordResetTokenInvalidException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(
            final IllegalArgumentException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(DateTimeException.class)
    ProblemDetail handleDateTimeException(
            final DateTimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
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
