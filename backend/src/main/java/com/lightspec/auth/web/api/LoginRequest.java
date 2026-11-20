package com.lightspec.auth.web.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "使用者名稱不可空白") String username,
        @NotBlank(message = "密碼不可空白") String password) {
}
