package com.lightspec.auth.service;

public class PasswordConfirmationException extends IllegalArgumentException {

    public PasswordConfirmationException() {
        super("密碼與確認密碼不一致");
    }
}
