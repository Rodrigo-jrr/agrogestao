package com.agrogestao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AgroGestaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgroGestaoApplication.class, args);
    }
}
