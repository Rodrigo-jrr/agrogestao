package com.agrogestao;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AgroGestaoApplication {

    /**
     * Sem isso, LocalDate.now()/LocalDateTime.now() (usados em todo o sistema, sem zona explícita) calculam
     * "hoje" no timezone do SO — UTC em muitos hosts de nuvem — em vez de Brasília. Entre 00:00 e 02:59 UTC
     * o servidor já estaria um dia na frente do usuário, rejeitando lançamentos válidos. Roda antes de
     * qualquer outra coisa na classe: um bloco estático é carregado assim que a classe é referenciada, o que
     * cobre tanto o main() (produção/dev) quanto o bootstrap do contexto nos testes com @SpringBootTest, que
     * usam esta classe como configuração mas nunca chamam main(). Para os testes (que também correm na mesma
     * JVM do Maven mas podem calcular "hoje" antes do contexto subir), o Surefire já recebe o timezone via
     * -Duser.timezone (pom.xml): isto aqui é o reforço para quando a aplicação roda fora do Maven.
     */
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AgroGestaoApplication.class, args);
    }
}
