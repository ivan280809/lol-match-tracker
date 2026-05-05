package com.loltracker.app.integration.riot;

import com.loltracker.app.player.RiotPlatform;

public interface RiotOperationsPort {

  String validateApiKey(RiotPlatform platform);
}
