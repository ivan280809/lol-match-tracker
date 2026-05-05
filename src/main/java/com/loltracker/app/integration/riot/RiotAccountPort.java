package com.loltracker.app.integration.riot;

public interface RiotAccountPort {

  boolean isConfigured();

  RiotAccount fetchAccount(String gameName, String tagLine);
}
