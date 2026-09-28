package com.nutrihealth.auth.adapter.in.web;

import com.nutrihealth.auth.domain.model.EmailAlreadyRegisteredException;
import com.nutrihealth.auth.domain.model.InvalidCredentialsException;
import com.nutrihealth.auth.domain.port.out.BiometricTokenVerifierPort;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(BiometricTokenVerifierPort.BiometricVerificationException.class)
    public ResponseEntity<Map<String, String>> handleBiometricVerificationFailed(
            BiometricTokenVerifierPort.BiometricVerificationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Biometric authentication failed"));
    }
}
