package com.example.jutjub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "isa.jutjub.repository")
@EntityScan(basePackages = "isa.jutjub.model")
@ComponentScan(basePackages = {"isa.jutjub"})
public class JutjubicApplication {
    public static void main(String[] args) {
        SpringApplication.run(JutjubicApplication.class, args);
    }
}
