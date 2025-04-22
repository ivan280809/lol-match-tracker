package com.loltracker.playerservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class PlayerServicesApplication {

  public static void main(String[] args) {
    SpringApplication.run(PlayerServicesApplication.class, args);
  }

  @GetMapping("/ping")
  public String ping() {
    return "pong";
  }
}
