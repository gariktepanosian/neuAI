package com.nutrihealth.auth.adapter.in.web;

import com.nutrihealth.auth.domain.port.in.AuthenticationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;

    public AuthController(AuthenticationUseCase authenticationUseCase) {
        this.authenticationUseCase = authenticationUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthenticationUseCase.RegisteredUser register(@Valid @RequestBody RegisterRequest request) {
        return authenticationUseCase.register(request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthenticationUseCase.IssuedToken login(@Valid @RequestBody LoginRequest request) {
        return authenticationUseCase.login(request.email(), request.password());
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8, max = 128) @NotBlank String password) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {
    }
}
