package com.lightspec.auth.service;

public class InvalidLoginException extends RuntimeException {

    public InvalidLoginException() {
        super("使用者名稱或密碼錯誤");
    }
}
