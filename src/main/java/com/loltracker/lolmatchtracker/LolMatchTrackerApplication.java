package com.loltracker.lolmatchtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.loltracker")
public class LolMatchTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(LolMatchTrackerApplication.class, args);
  }
}
