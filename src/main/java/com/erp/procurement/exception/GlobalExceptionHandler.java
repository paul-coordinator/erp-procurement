package com.erp.procurement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

// ─── Domain Exceptions ────────────────────────────────
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

class InvalidWorkflowTransitionException extends RuntimeException {
    public InvalidWorkflowTransitionException(String message) { super(message); }
}

class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) { super(message); }
}

class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

// ─── Global Exception Handler ─────────────────────────
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    public ResponseEntity<?> handleWorkflow(InvalidWorkflowTransitionException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<?> handleBusiness(BusinessRuleViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccess(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied: insufficient role permissions");
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> handleConcurrency(Exception ex) {
        return error(HttpStatus.CONFLICT,
                "This record was modified by another user. Please refresh and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
