package com.loltracker.matchhistoryservice.domain.ports.out;

public interface NotificationPort {
  void notifyMatchHistory(String message);
}
