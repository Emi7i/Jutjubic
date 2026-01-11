package com.example.springcombined;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.example.springcombined", "isa.vezbe1.rest_example"})
public class JutjubicApplication {
    public static void main(String[] args) {
        SpringApplication.run(JutjubicApplication.class, args);
    }
}
