package com.lightspec.auth.service;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("使用者名稱已被使用");
    }
}
