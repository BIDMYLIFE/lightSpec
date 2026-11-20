package com.lightspec.auth.service;

public class AdminAlreadyExistsException extends RuntimeException {

    public AdminAlreadyExistsException() {
        super("管理員已完成初始化");
    }
}
