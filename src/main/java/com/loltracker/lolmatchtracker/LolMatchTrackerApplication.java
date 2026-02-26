package com.loltracker.lolmatchtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.loltracker")
@EnableJpaRepositories(basePackages = "com.loltracker")
@EntityScan(basePackages = "com.loltracker")
public class LolMatchTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(LolMatchTrackerApplication.class, args);
  }
}
