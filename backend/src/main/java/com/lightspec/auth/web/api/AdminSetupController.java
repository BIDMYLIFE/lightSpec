package com.lightspec.auth.web.api;

import com.lightspec.auth.service.AdminInitializationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/setup")
public class AdminSetupController {

    private final AdminInitializationService adminInitializationService;

    public AdminSetupController(AdminInitializationService adminInitializationService) {
        this.adminInitializationService = adminInitializationService;
    }

    @PostMapping("/admin")
    public ResponseEntity<AdminSetupResponse> createAdmin(@Valid @RequestBody AdminSetupRequest request) {
        var admin = adminInitializationService.initialize(
                request.username(), request.password(), request.passwordConfirmation());
        return ResponseEntity.created(URI.create("/api/auth/me"))
                .body(new AdminSetupResponse(admin.getUsername(), admin.getRole().name()));
    }

    public record AdminSetupResponse(String username, String role) {
    }
}
