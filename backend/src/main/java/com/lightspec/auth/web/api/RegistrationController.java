package com.lightspec.auth.web.api;

import com.lightspec.auth.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final UserRegistrationService registrationService;

    public RegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        var user = registrationService.register(
                request.username(), request.password(), request.passwordConfirmation());
        return ResponseEntity.created(URI.create("/login"))
                .body(new RegistrationResponse(user.getUsername(), user.getRole().name()));
    }

    public record RegistrationResponse(String username, String role) {
    }
}
