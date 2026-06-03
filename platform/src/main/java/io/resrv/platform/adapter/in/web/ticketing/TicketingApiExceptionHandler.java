package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.purchase.TicketPurchaseAccessDeniedException;
import io.resrv.ticketing.application.purchase.TicketPurchaseIdempotencyException;
import io.resrv.ticketing.application.purchase.TicketPurchaseValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = TicketingPurchaseWebAdapter.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class TicketingApiExceptionHandler {

    @ExceptionHandler(TicketPurchaseValidationException.class)
    ProblemDetail handleValidation(
            final TicketPurchaseValidationException exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(TicketPurchaseAccessDeniedException.class)
    ProblemDetail handleAccessDenied(
            final TicketPurchaseAccessDeniedException exception, final HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(TicketPurchaseIdempotencyException.class)
    ProblemDetail handleIdempotency(
            final TicketPurchaseIdempotencyException exception, final HttpServletRequest request) {
        final var problem = problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
        problem.setProperty("reason", exception.reason().name());
        return problem;
    }

    private static ProblemDetail problem(
            final HttpStatus status, final String detail, final HttpServletRequest request) {
        final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
