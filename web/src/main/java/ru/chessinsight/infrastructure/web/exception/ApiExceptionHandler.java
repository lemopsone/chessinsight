package ru.chessinsight.infrastructure.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.exception.ApplicationException;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.training.service.exception.ScenarioAccessException;
import ru.chessinsight.application.game.training.service.exception.ScenarioNotFoundException;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.exception.DomainException;
import ru.chessinsight.infrastructure.web.dto.ProblemDetails;

import java.sql.SQLException;
import java.util.StringJoiner;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleInvalidArgument(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String detail = buildValidationDetail(ex.getBindingResult().getFieldErrors(),
                ex.getBindingResult().getGlobalErrors());
        return badRequest(detail, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetails> handleBindException(
            BindException ex,
            HttpServletRequest request
    ) {
        String detail = buildValidationDetail(ex.getBindingResult().getFieldErrors(),
                ex.getBindingResult().getGlobalErrors());
        return badRequest(detail, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        StringJoiner joiner = new StringJoiner("; ");
        ex.getConstraintViolations().forEach(v ->
                joiner.add(v.getPropertyPath() + ": " + v.getMessage()));
        return badRequest(joiner.toString(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetails> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return badRequest("Missing parameter: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetails> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return badRequest("Invalid value for parameter: " + ex.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return badRequest("Invalid request body", request);
    }

    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ProblemDetails> handleUserExists(
            UserExistsException ex,
            HttpServletRequest request
    ) {
        return conflict(ex.getMessage(), request);
    }

    @ExceptionHandler({WrongCredentialsException.class, AuthException.class})
    public ResponseEntity<ProblemDetails> handleUnauthorized(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return unauthorized(ex.getMessage(), request);
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            GameNotFoundException.class,
            ScenarioNotFoundException.class,
            ScenarioAccessException.class
    })
    public ResponseEntity<ProblemDetails> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return notFound(ex.getMessage(), request);
    }

    @ExceptionHandler({ApplicationException.class, DomainException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetails> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return badRequest(ex.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetails> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String detail = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status)
                .body(ProblemDetailsFactory.create(status, detail, request.getRequestURI()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetails> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        if (isReadOnlyViolation(ex)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ProblemDetailsFactory.create(
                            HttpStatus.FORBIDDEN,
                            "Read-only replica: write operations are not allowed",
                            request.getRequestURI()
                    ));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetailsFactory.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Database error",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetailsFactory.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error",
                        request.getRequestURI()
                ));
    }

    private boolean isReadOnlyViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("25006".equals(sqlState) || "42501".equals(sqlState)) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("read-only")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ProblemDetails> badRequest(String detail, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ProblemDetailsFactory.create(HttpStatus.BAD_REQUEST, detail, request.getRequestURI()));
    }

    private ResponseEntity<ProblemDetails> conflict(String detail, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetailsFactory.create(HttpStatus.CONFLICT, detail, request.getRequestURI()));
    }

    private ResponseEntity<ProblemDetails> unauthorized(String detail, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetailsFactory.create(HttpStatus.UNAUTHORIZED, detail, request.getRequestURI()));
    }

    private ResponseEntity<ProblemDetails> notFound(String detail, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetailsFactory.create(HttpStatus.NOT_FOUND, detail, request.getRequestURI()));
    }

    private String buildValidationDetail(
            java.util.List<org.springframework.validation.FieldError> fields,
            java.util.List<org.springframework.validation.ObjectError> globals
    ) {
        StringJoiner joiner = new StringJoiner("; ");
        for (var field : fields) {
            joiner.add(field.getField() + ": " + field.getDefaultMessage());
        }
        for (var global : globals) {
            joiner.add(global.getObjectName() + ": " + global.getDefaultMessage());
        }
        return joiner.toString();
    }
}
