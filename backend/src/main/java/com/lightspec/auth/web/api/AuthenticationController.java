package com.lightspec.auth.web.api;

import com.lightspec.auth.service.AuthCookieService;
import com.lightspec.auth.service.AuthenticationService;
import com.lightspec.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final AuthCookieService authCookies;

    public AuthenticationController(
            AuthenticationService authenticationService,
            JwtService jwtService,
            AuthCookieService authCookies) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.authCookies = authCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = authenticationService.authenticate(request.username(), request.password());
        var cookie = authCookies.create(jwtService.issue(user));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new CurrentUserResponse(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        return new CurrentUserResponse(
                Integer.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("role"));
    }

    public record CurrentUserResponse(Integer id, String username, String role) {
    }
}
