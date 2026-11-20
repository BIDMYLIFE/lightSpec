package com.lightspec.auth.web.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "使用者名稱不可空白")
        @Size(max = 50, message = "使用者名稱不可超過 50 個字元")
        String username,

        @NotBlank(message = "密碼不可空白")
        @Size(min = 8, max = 128, message = "密碼長度須為 8 到 128 個字元")
        String password,

        @NotBlank(message = "確認密碼不可空白")
        String passwordConfirmation) {
}
