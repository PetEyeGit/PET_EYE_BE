package com.sang.sourcepattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaRepositories(basePackages = "com.sang.sourcepattern.repository")
public class PET_EYE_BE {

    public static void main(String[] args) {
        SpringApplication.run(PET_EYE_BE.class, args);
    }

}
 