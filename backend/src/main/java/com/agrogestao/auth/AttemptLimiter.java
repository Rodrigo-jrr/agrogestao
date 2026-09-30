package com.agrogestao.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janela deslizante de tentativas por chave (ex.: "login-conta:fulano@x.com", "login-ip:1.2.3.4").
 * Fica em memória: vale por instância da aplicação e zera ao reiniciar — suficiente para barrar
 * tentativa e erro; com várias instâncias, trocar por um armazenamento compartilhado (ex.: Redis).
 */
public class AttemptLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Se a chave está bloqueada, quanto tempo falta para liberar. */
    public Optional<Duration> blockedFor(String key) {
        Deque<Instant> times = attempts.get(key);
        if (times == null) {
            return Optional.empty();
        }
        synchronized (times) {
            prune(times);
            if (times.size() < maxAttempts) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(clock.instant(), times.peekFirst().plus(window)));
        }
    }

    public void record(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(times -> {
                synchronized (times) {
                    prune(times);
                    return times.isEmpty();
                }
            });
        }
        Deque<Instant> times = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (times) {
            prune(times);
            times.addLast(clock.instant());
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void prune(Deque<Instant> times) {
        Instant limit = clock.instant().minus(window);
        while (!times.isEmpty() && times.peekFirst().isBefore(limit)) {
            times.pollFirst();
        }
    }
}
