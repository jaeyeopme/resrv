package io.resrv.platform.application.auth.in;

public record ResetPasswordResult(boolean reset) {

    public static ResetPasswordResult success() {
        return new ResetPasswordResult(true);
    }
}
