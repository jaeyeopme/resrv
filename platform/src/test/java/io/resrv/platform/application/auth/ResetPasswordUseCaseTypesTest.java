package io.resrv.platform.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.application.auth.in.ResetPasswordCommand;
import io.resrv.platform.application.auth.in.ResetPasswordResult;
import org.junit.jupiter.api.Test;

class ResetPasswordUseCaseTypesTest {

    @Test
    void resetPasswordCommandRequiresTokenAndPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResetPasswordCommand(null, "new-passw0rd!"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResetPasswordCommand(" ", "new-passw0rd!"));
        assertThrows(IllegalArgumentException.class, () -> new ResetPasswordCommand("token", null));
        assertThrows(IllegalArgumentException.class, () -> new ResetPasswordCommand("token", " "));
    }

    @Test
    void resetPasswordResultCanRepresentSuccess() {
        final var result = ResetPasswordResult.success();

        assertTrue(result.reset());
        assertEquals(new ResetPasswordResult(true), result);
    }
}
