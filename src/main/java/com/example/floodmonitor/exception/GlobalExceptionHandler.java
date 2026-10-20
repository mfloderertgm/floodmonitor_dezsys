package com.example.floodmonitor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return createError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUnknownPath(NoResourceFoundException exception, HttpServletRequest request) {
        return createError(HttpStatus.NOT_FOUND, "Dieser Pfad existiert nicht", request);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return createError(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleWrongParameterType(MethodArgumentTypeMismatchException exception,
                                                  HttpServletRequest request) {
        String message = "Der Parameter '" + exception.getName() + "' hat einen ungültigen Wert: "
                + exception.getValue();
        return createError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleWrongMethod(HttpRequestMethodNotSupportedException exception,
                                           HttpServletRequest request) {
        String message = "Die Methode " + exception.getMethod() + " wird hier nicht unterstützt";
        return createError(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    // Der Client wollte ein Format, das wir nicht liefern können. Deshalb wird hier JSON
    // fest vorgegeben, sonst könnte Spring auch den Fehler selbst nicht ausgeben.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception,
                                                             HttpServletRequest request) {
        String message = "Format nicht unterstützt. Erlaubt sind application/json und application/xml";
        ErrorResponse error = createError(HttpStatus.NOT_ACCEPTABLE, message, request);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception exception, HttpServletRequest request) {
        return createError(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", request);
    }

    private ErrorResponse createError(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
    }
}
