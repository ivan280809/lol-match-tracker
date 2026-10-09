package com.loltracker.lolmatchtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.loltracker.app")
@Import(com.loltracker.app.ObjectMapperConfiguration.class)
@EnableJpaRepositories(basePackages = "com.loltracker.app")
@EntityScan(basePackages = "com.loltracker.app")
@EnableScheduling
public class LolMatchTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(LolMatchTrackerApplication.class, args);
  }
}
