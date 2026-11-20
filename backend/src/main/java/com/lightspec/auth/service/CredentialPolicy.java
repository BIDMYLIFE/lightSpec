package com.lightspec.auth.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class CredentialPolicy {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    public NormalizedUsername normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            throw new IllegalArgumentException("使用者名稱為必填欄位");
        }

        var display = Normalizer.normalize(rawUsername.strip(), Normalizer.Form.NFKC);
        var length = display.codePointCount(0, display.length());
        if (length < MIN_USERNAME_LENGTH || length > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("使用者名稱長度必須介於 3 到 50 個字元");
        }
        if (!display.codePoints().allMatch(this::isAllowedUsernameCodePoint)) {
            throw new IllegalArgumentException("使用者名稱只能包含文字、數字、底線、連字號或句點");
        }

        return new NormalizedUsername(display, display.toLowerCase(Locale.ROOT));
    }

    public void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("密碼為必填欄位");
        }
        var length = password.codePointCount(0, password.length());
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密碼長度必須介於 8 到 128 個字元");
        }
        if (password.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("密碼不可包含控制字元");
        }
    }

    private boolean isAllowedUsernameCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint)
                || codePoint == '_'
                || codePoint == '-'
                || codePoint == '.';
    }

    public record NormalizedUsername(String display, String canonical) {
    }
}
