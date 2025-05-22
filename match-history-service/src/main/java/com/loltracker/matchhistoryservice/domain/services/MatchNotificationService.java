package com.loltracker.matchhistoryservice.domain.services;

import org.springframework.stereotype.Service;

@Service
public class MatchNotificationService {

  public void notifyMatchHistory(String gameName, String endOfGameResult, String gameMode) {
    System.out.println("Game Name: " + gameName);
    System.out.println("End of Game Result: " + endOfGameResult);
    System.out.println("Game Mode: " + gameMode);
    System.out.println();
  }
}
