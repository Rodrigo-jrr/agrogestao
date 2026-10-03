package com.agrogestao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * O servidor precisa calcular "hoje" no horário de Brasília, não no do SO que o executa (em nuvem, UTC é
 * comum). Sem isso, entre 00:00 e 02:59 UTC o servidor já acha que é o dia seguinte enquanto o Brasil ainda
 * está no dia anterior, e qualquer validação que compare contra "hoje" ({@code LocalDate.now()},
 * {@code LocalDateTime.now()}) rejeita lançamentos válidos. Corrigido fixando o timezone default da JVM
 * (ver {@code AgroGestaoApplication} e o {@code argLine} do Surefire no pom.xml) em vez de tocar cada
 * ponto que chama {@code .now()}.
 */
class JvmTimezoneTest {

    @Test
    void jvmDefaultTimezoneIsSaoPaulo() {
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    void nowWithoutAnExplicitZoneMatchesSaoPauloRegardlessOfTheHostClock() {
        assertThat(LocalDate.now()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    }
}
