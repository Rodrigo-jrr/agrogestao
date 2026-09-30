package com.agrogestao.auth;

import com.agrogestao.common.exception.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Protege login e cadastro contra tentativa e erro:
 * - por conta: poucas senhas erradas seguidas bloqueiam aquele e-mail por um tempo;
 * - por IP: muitas falhas (em contas variadas) ou muitos cadastros bloqueiam a origem.
 */
@Service
public class LoginProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);

    private final AttemptLimiter failuresByAccount;
    private final AttemptLimiter failuresByIp;
    private final AttemptLimiter registrationsByIp;

    public LoginProtectionService(
            @Value("${agrogestao.security.login.max-failures-per-account:5}") int maxPerAccount,
            @Value("${agrogestao.security.login.max-failures-per-ip:20}") int maxPerIp,
            @Value("${agrogestao.security.login.window-minutes:15}") int windowMinutes,
            @Value("${agrogestao.security.register.max-per-ip-per-hour:10}") int maxRegistrationsPerIp) {
        Clock clock = Clock.systemUTC();
        Duration window = Duration.ofMinutes(windowMinutes);
        this.failuresByAccount = new AttemptLimiter(maxPerAccount, window, clock);
        this.failuresByIp = new AttemptLimiter(maxPerIp, window, clock);
        this.registrationsByIp = new AttemptLimiter(maxRegistrationsPerIp, Duration.ofHours(1), clock);
    }

    public void checkLoginAllowed(String email, String ip) {
        failuresByAccount.blockedFor(accountKey(email))
                .or(() -> failuresByIp.blockedFor(ip))
                .ifPresent(wait -> {
                    throw new TooManyRequestsException(
                            "Muitas tentativas de login. Tente novamente em " + minutes(wait)
                                    + " ou use \"Esqueci minha senha\".");
                });
    }

    public void loginFailed(String email, String ip) {
        failuresByAccount.record(accountKey(email));
        failuresByIp.record(ip);
        if (failuresByAccount.blockedFor(accountKey(email)).isPresent()) {
            // Sem o e-mail completo no log (dado pessoal); o suficiente para investigar ataques.
            log.warn("Login bloqueado temporariamente após falhas seguidas (ip={})", ip);
        }
    }

    public void loginSucceeded(String email) {
        failuresByAccount.reset(accountKey(email));
    }

    public void checkRegistrationAllowed(String ip) {
        registrationsByIp.blockedFor(ip).ifPresent(wait -> {
            throw new TooManyRequestsException(
                    "Muitos cadastros feitos a partir desta conexão. Tente novamente em " + minutes(wait) + ".");
        });
        registrationsByIp.record(ip);
    }

    private static String accountKey(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String minutes(Duration wait) {
        long minutes = Math.max(1, (wait.getSeconds() + 59) / 60);
        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }
}
