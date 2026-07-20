package com.kuocai.cdn.integration.scdn;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ScdnInternalController.class)
public class ScdnIntegrationExceptionHandler {

    @ExceptionHandler(ScdnIntegrationException.class)
    public ResponseEntity<ScdnContracts.Envelope<Void>> integrationError(ScdnIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ScdnContracts.Envelope.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ScdnContracts.Envelope<Void>> validationError(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ScdnContracts.Envelope.failure("INVALID_REQUEST", "Request validation failed"));
    }
}

