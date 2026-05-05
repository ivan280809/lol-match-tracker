package com.loltracker.app.integration.riot;

import com.loltracker.app.player.RiotPlatform;
import java.util.List;

public interface RiotRankPort {

  List<RiotRankEntry> fetchRankEntries(RiotPlatform platform, String puuid);
}
