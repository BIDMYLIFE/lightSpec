package com.lightspec.auth.web.api;

import com.lightspec.auth.service.AdminAlreadyExistsException;
import com.lightspec.auth.service.SetupRequiredException;
import com.lightspec.auth.service.InvalidLoginException;
import com.lightspec.auth.service.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(AdminAlreadyExistsException.class)
    ProblemDetail adminAlreadyExists(AdminAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "admin-already-exists", "管理員已存在", exception.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail usernameAlreadyExists(UsernameAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "username-already-exists", "使用者名稱已存在", exception.getMessage());
    }

    @ExceptionHandler(SetupRequiredException.class)
    ProblemDetail setupRequired(SetupRequiredException exception) {
        return problem(HttpStatus.CONFLICT, "setup-required", "需要初始化", exception.getMessage());
    }

    @ExceptionHandler(InvalidLoginException.class)
    ProblemDetail invalidLogin(InvalidLoginException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-login", "登入失敗", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidCredentials(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-credentials", "輸入資料無效", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var problem = problem(HttpStatus.BAD_REQUEST, "validation-failed", "欄位驗證失敗", "請修正標示的欄位");
        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:lightspec:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
