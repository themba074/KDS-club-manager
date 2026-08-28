package com.kds.backend.identity.api;

import com.kds.backend.identity.application.*;
import com.kds.backend.identity.domain.PasswordResetTokenEntity;
import com.kds.backend.identity.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class SessionConcurrencyTests {
    @Autowired AuthService auth;
    @Autowired SecretTokenService secrets;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;

    @Test void resetTokenCanOnlyBeConsumedOnceUnderConcurrency() throws Exception {
        TokenPair account = account();
        String token = resetToken(account);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> confirm(start, token, "first-new-password"));
            var second = executor.submit(() -> confirm(start, token, "second-new-password"));
            start.countDown();
            boolean firstWon = first.get(30, TimeUnit.SECONDS);
            boolean secondWon = second.get(30, TimeUnit.SECONDS);
            assertNotEquals(firstWon, secondWon, "Exactly one reset must succeed");
            assertNotNull(auth.login(account.email(), firstWon ? "first-new-password" : "second-new-password"));
            assertThrows(AuthenticationException.class, () -> auth.confirmPasswordReset(token, "third-new-password"));
            assertThrows(AuthenticationException.class, () -> auth.refresh(account.refreshToken()));
        }
    }
    @Test void logoutWithRotatedCookieRevokesDescendantsButNotOtherSessions() {
        TokenPair account = account();
        TokenPair separateSession = auth.login(account.email(), "original-password");
        TokenPair rotated = auth.refresh(account.refreshToken());
        auth.logout(account.refreshToken());
        assertThrows(AuthenticationException.class, () -> auth.refresh(rotated.refreshToken()));
        assertNotNull(auth.refresh(separateSession.refreshToken()));
    }
    @Test void concurrentRefreshCannotSurviveLogout() throws Exception {
        TokenPair account = account();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var refresh = executor.submit(() -> refresh(start, account.refreshToken()));
            var logout = executor.submit(() -> { start.await(); auth.logout(account.refreshToken()); return true; });
            start.countDown();
            TokenPair replacement = refresh.get(30, TimeUnit.SECONDS);
            assertTrue(logout.get(30, TimeUnit.SECONDS));
            if (replacement != null) assertThrows(AuthenticationException.class, () -> auth.refresh(replacement.refreshToken()));
            assertThrows(AuthenticationException.class, () -> auth.refresh(account.refreshToken()));
        }
    }
    @Test void concurrentRefreshCannotEscapePasswordResetRevocation() throws Exception {
        TokenPair account = account();
        String token = resetToken(account);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var refresh = executor.submit(() -> refresh(start, account.refreshToken()));
            var reset = executor.submit(() -> confirm(start, token, "replacement-password"));
            start.countDown();
            TokenPair replacement = refresh.get(30, TimeUnit.SECONDS);
            assertTrue(reset.get(30, TimeUnit.SECONDS));
            if (replacement != null) assertThrows(AuthenticationException.class, () -> auth.refresh(replacement.refreshToken()));
        }
    }
    private boolean confirm(CountDownLatch start, String token, String password) throws Exception {
        start.await();
        try { auth.confirmPasswordReset(token, password); return true; }
        catch (AuthenticationException rejected) { return false; }
    }
    private TokenPair refresh(CountDownLatch start, String token) throws Exception {
        start.await();
        try { return auth.refresh(token); }
        catch (AuthenticationException rejected) { return null; }
    }
    private TokenPair account() { return auth.register(UUID.randomUUID() + "@example.test", "original-password"); }
    private String resetToken(TokenPair account) {
        String token = secrets.generate();
        resetTokens.saveAndFlush(new PasswordResetTokenEntity(UUID.randomUUID(),
            users.findById(account.userId()).orElseThrow(), secrets.hash(token),
            Instant.now().plusSeconds(300), Instant.now()));
        return token;
    }
}
