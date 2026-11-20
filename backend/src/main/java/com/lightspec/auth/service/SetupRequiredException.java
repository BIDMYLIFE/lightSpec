package com.lightspec.auth.service;

public class SetupRequiredException extends RuntimeException {

    public SetupRequiredException() {
        super("請先完成管理員初始化");
    }
}
