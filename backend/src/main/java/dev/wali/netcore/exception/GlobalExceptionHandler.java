package dev.wali.netcore.exception;

import dev.wali.netcore.domain.IpAddressStatus;
import dev.wali.netcore.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDeviceNotFound(
            DeviceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(NetworkInterfaceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInterfaceNotFound(
            NetworkInterfaceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "INTERFACE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(SubnetNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSubnetNotFound(
            SubnetNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "SUBNET_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(IpAddressNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAddressNotFound(
            IpAddressNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "IP_ADDRESS_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(NoAvailableIpAddressException.class)
    public ResponseEntity<ApiErrorResponse> handlePoolExhausted(
            NoAvailableIpAddressException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "IP_POOL_EXHAUSTED", exception, request);
    }

    @ExceptionHandler(IpAddressStateConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAddressConflict(
            IpAddressStateConflictException exception,
            HttpServletRequest request
    ) {
        String code = switch (exception.getStatus()) {
            case RESERVED -> "IP_RESERVED";
            case ALLOCATED -> "IP_ALREADY_ALLOCATED";
            default -> "IP_STATE_CONFLICT";
        };
        return error(HttpStatus.CONFLICT, code, exception, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "A resource with the same unique value already exists",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            Exception exception,
            HttpServletRequest request
    ) {
        return error(status, code, exception.getMessage(), request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
